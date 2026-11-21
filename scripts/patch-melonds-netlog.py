#!/usr/bin/env python3
"""Vá lõi melonDS (src/net/Net_Slirp.cpp) để ghi nhật ký chi tiết gói tin mạng, phục vụ gỡ lỗi chơi online.

Mỗi gói IPv4 đi qua mạng ảo (game -> mạng và mạng -> game) được ghi một dòng:
  net g>n TCP 10.64.0.16:1030 > 78.46.231.155:80 [SA] len=120 hex=47 45 54 20 ... GET / HTTP/1.1|Host: ...
DNS (UDP cổng 53) ghi thêm tên miền hỏi và các địa chỉ A trả về.
Chạy lại nhiều lần cũng an toàn. Thiếu mẫu cần vá thì báo lỗi để CI đỏ, không vá âm thầm.
"""
import sys

path = sys.argv[1]
s = open(path, encoding="utf-8").read()
MARK = "/* retro-netlog patch */"
if MARK in s:
    print("Net_Slirp.cpp đã được vá")
    sys.exit(0)

FUNC = MARK + r'''
static void RetroNetLog(const char* dir, const u8* b, size_t len)
{
    if (len < 14) return;
    unsigned et = (b[12] << 8) | b[13];
    if (et != 0x0800)
    {
        Log(LogLevel::Debug, "net %s ethertype %04X len=%zu\n", dir, et, len);
        return;
    }
    if (len < 34) return;
    size_t ihl = (b[14] & 0xF) * 4;
    unsigned proto = b[23];
    size_t total = (b[16] << 8) | b[17];
    if (total + 14 > len) total = len - 14;
    char src[24], dst[24];
    snprintf(src, sizeof src, "%u.%u.%u.%u", b[26], b[27], b[28], b[29]);
    snprintf(dst, sizeof dst, "%u.%u.%u.%u", b[30], b[31], b[32], b[33]);
    if (ihl < 20 || total < ihl || 14 + ihl + 8 > len)
    {
        Log(LogLevel::Debug, "net %s IP proto=%u %s > %s len=%zu\n", dir, proto, src, dst, len);
        return;
    }
    const u8* t = b + 14 + ihl;
    size_t tl = total - ihl;

    if (proto == 6 && tl >= 20)
    {
        unsigned sp = (t[0] << 8) | t[1], dp = (t[2] << 8) | t[3];
        unsigned flags = t[13];
        size_t off = (t[12] >> 4) * 4;
        size_t pl = tl > off ? tl - off : 0;
        char fl[8];
        int n = 0;
        if (flags & 0x02) fl[n++] = 'S';
        if (flags & 0x10) fl[n++] = 'A';
        if (flags & 0x08) fl[n++] = 'P';
        if (flags & 0x01) fl[n++] = 'F';
        if (flags & 0x04) fl[n++] = 'R';
        fl[n] = 0;
        char hex[40] = "", txt[104] = "";
        if (pl > 0 && off + 14 + ihl <= len)
        {
            const u8* p = t + off;
            size_t avail = len - (14 + ihl + off);
            size_t m = pl < avail ? pl : avail;
            for (size_t i = 0; i < m && i < 8; i++) snprintf(hex + i * 3, 4, "%02X ", p[i]);
            size_t k = 0;
            for (size_t i = 0; i < m && k < 100; i++)
            {
                u8 c = p[i];
                txt[k++] = (c == '\r') ? ' ' : (c == '\n') ? '|' : (c >= 0x20 && c < 0x7F) ? (char)c : '.';
            }
            txt[k] = 0;
        }
        Log(LogLevel::Debug, "net %s TCP %s:%u > %s:%u [%s] len=%zu %s%s\n", dir, src, sp, dst, dp, fl, pl, hex, txt);
    }
    else if (proto == 17)
    {
        unsigned sp = (t[0] << 8) | t[1], dp = (t[2] << 8) | t[3];
        size_t pl = tl > 8 ? tl - 8 : 0;
        char info[120] = "";
        if ((sp == 53 || dp == 53) && pl >= 13 && 14 + ihl + 8 + pl <= len)
        {
            const u8* u = t + 8;
            char name[64];
            size_t k = 0, p = 12;
            while (p < pl && u[p] && k < sizeof name - 2)
            {
                size_t l = u[p++];
                for (size_t i = 0; i < l && p < pl && k < sizeof name - 2; i++) name[k++] = (char)u[p++];
                name[k++] = '.';
            }
            name[k] = 0;
            unsigned qr = u[2] >> 7, rcode = u[3] & 0xF, an = (u[6] << 8) | u[7];
            int w = snprintf(info, sizeof info, "DNS %s %s rcode=%u", qr ? "reply" : "query", name, rcode);
            if (qr)
            {
                while (p < pl && u[p]) p += 1 + u[p];  // bỏ tên câu hỏi (nếu chưa gặp byte 0)
                p += 5;
                for (unsigned a = 0; a < an && a < 4 && p + 12 <= pl; a++)
                {
                    p += ((u[p] & 0xC0) == 0xC0) ? 2 : 1;
                    if (p + 10 > pl) break;
                    unsigned type = (u[p] << 8) | u[p + 1], rd = (u[p + 8] << 8) | u[p + 9];
                    p += 10;
                    if (type == 1 && rd == 4 && p + 4 <= pl && w > 0 && (size_t)w < sizeof info - 20)
                        w += snprintf(info + w, sizeof info - w, " A=%u.%u.%u.%u", u[p], u[p + 1], u[p + 2], u[p + 3]);
                    p += rd;
                }
            }
        }
        Log(LogLevel::Debug, "net %s UDP %s:%u > %s:%u len=%zu %s\n", dir, src, sp, dst, dp, pl, info);
    }
    else
    {
        Log(LogLevel::Debug, "net %s IP proto=%u %s > %s len=%zu\n", dir, proto, src, dst, total);
    }
}

'''

# 1) Hàm ghi log, đặt ngay trước SlirpCbSendPacket.
anchor = "ssize_t Net_Slirp::SlirpCbSendPacket("
if s.count(anchor) != 1:
    sys.exit("Không thấy SlirpCbSendPacket trong Net_Slirp.cpp (lõi đổi cấu trúc?)")
s = s.replace(anchor, FUNC + anchor)

# 2) Gói mạng -> game: thay dòng log kích thước cũ.
old = '    Log(LogLevel::Debug, "slirp: response packet of %zu bytes, type %04X\\n", len, ntohs(((u16*)buf)[6]));\n'
if s.count(old) != 1:
    sys.exit("Không thấy dòng log 'slirp: response packet' (lõi đổi cấu trúc?)")
s = s.replace(old, '    RetroNetLog("n>g", (const u8*)buf, len);\n')

# 3) Gói game -> mạng: ghi ngay đầu SendPacket (sau kiểm tra kích thước).
old = "    u16 ethertype = ntohs(*(u16*)&data[0xC]);\n"
if s.count(old) != 1:
    sys.exit("Không thấy khai báo ethertype trong Net_Slirp::SendPacket (lõi đổi cấu trúc?)")
s = s.replace(old, '    RetroNetLog("g>n", data, (size_t)len);\n\n' + old)

open(path, "w", encoding="utf-8").write(s)
print("Đã vá Net_Slirp.cpp: ghi nhật ký gói tin mạng")
