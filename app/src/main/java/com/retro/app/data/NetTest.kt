package com.retro.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Kiểm tra kết nối tới máy chủ chơi online NDS, chạy bằng mạng của điện thoại (không cần mở game):
 * hỏi DNS của máy chủ xem [HOST] trỏ về IP nào, rồi thử kết nối TCP cổng 80 tới IP đó.
 */
object NetTest {
    const val HOST = "nas.nintendowifi.net"

    /** Kết quả một lần thử: [ok] = đủ DNS + kết nối; [text] = mô tả một dòng cho người dùng. */
    data class Result(val ok: Boolean, val text: String)

    /** Thử máy chủ có DNS là [dnsIp]. */
    suspend fun testServer(dnsIp: String): Result = withContext(Dispatchers.IO) {
        val ips = try {
            queryDns(dnsIp)
        } catch (e: java.net.SocketTimeoutException) {
            return@withContext Result(false, "DNS không trả lời sau 3 giây (có thể bị chặn)")
        } catch (e: Exception) {
            return@withContext Result(false, "Không gửi được truy vấn DNS: ${e.javaClass.simpleName}")
        }
        if (ips.isEmpty()) return@withContext Result(false, "DNS trả lời nhưng không có địa chỉ cho $HOST")
        val ip = ips.first()
        ofConnect(ip, tryConnect(ip))
    }

    /** Thử bằng DNS mặc định của điện thoại, để so sánh (thường trả về địa chỉ khác hoặc không có). */
    suspend fun testPhoneDns(): Result = withContext(Dispatchers.IO) {
        try {
            val ip = InetAddress.getAllByName(HOST).firstOrNull { it.address.size == 4 }?.hostAddress
                ?: return@withContext Result(false, "Không có địa chỉ IPv4")
            Result(true, "→ $ip")
        } catch (e: Exception) {
            Result(false, "Không phân giải được (bình thường, vì máy chủ gốc của Nintendo đã đóng)")
        }
    }

    private fun ofConnect(ip: String, c: String?): Result =
        if (c == null) Result(false, "DNS → $ip, nhưng không kết nối được tới $ip:80 (quá 4 giây)")
        else Result(true, "DNS → $ip, kết nối được${if (c.isNotBlank()) " ($c)" else ""}")

    /** Chuỗi dòng trạng thái HTTP (có thể rỗng) nếu kết nối được, null nếu không. */
    private fun tryConnect(ip: String): String? = try {
        Socket().use { s ->
            s.connect(InetSocketAddress(ip, 80), 4000)
            s.soTimeout = 3000
            try {
                s.getOutputStream().write("GET / HTTP/1.0\r\nHost: $HOST\r\n\r\n".toByteArray())
                s.getOutputStream().flush()
                val buf = ByteArray(64)
                val n = s.getInputStream().read(buf)
                if (n > 0) String(buf, 0, n, Charsets.ISO_8859_1).lineSequence().first().trim().take(40) else ""
            } catch (e: Exception) {
                ""
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun queryDns(dnsIp: String): List<String> {
        val id = (System.nanoTime() and 0x7FFF).toInt()
        val out = java.io.ByteArrayOutputStream()
        out.write(id shr 8); out.write(id and 0xFF)
        out.write(0x01); out.write(0x00)  // cờ: truy vấn chuẩn, cần đệ quy
        out.write(0); out.write(1)        // 1 câu hỏi
        repeat(6) { out.write(0) }        // 0 trả lời / quyền hạn / bổ sung
        for (label in HOST.split('.')) {
            out.write(label.length)
            out.write(label.toByteArray())
        }
        out.write(0)
        out.write(0); out.write(1)  // loại A
        out.write(0); out.write(1)  // lớp IN
        val q = out.toByteArray()
        DatagramSocket().use { sock ->
            sock.soTimeout = 3000
            sock.send(DatagramPacket(q, q.size, InetAddress.getByName(dnsIp), 53))
            val buf = ByteArray(512)
            val pkt = DatagramPacket(buf, buf.size)
            sock.receive(pkt)
            return parseAnswers(buf, pkt.length)
        }
    }

    /** Lấy các địa chỉ IPv4 (bản ghi A) trong gói trả lời DNS. */
    private fun parseAnswers(b: ByteArray, len: Int): List<String> {
        if (len < 12) return emptyList()
        if ((b[3].toInt() and 0x0F) != 0) return emptyList()  // mã lỗi khác 0
        val an = ((b[6].toInt() and 0xFF) shl 8) or (b[7].toInt() and 0xFF)
        var p = 12
        fun skipName() {
            while (p < len) {
                val l = b[p].toInt() and 0xFF
                if (l == 0) { p++; return }
                if (l and 0xC0 == 0xC0) { p += 2; return }
                p += 1 + l
            }
        }
        skipName(); p += 4  // câu hỏi: tên + loại + lớp
        val ips = ArrayList<String>()
        repeat(an) {
            if (p >= len) return ips
            skipName()
            if (p + 10 > len) return ips
            val type = ((b[p].toInt() and 0xFF) shl 8) or (b[p + 1].toInt() and 0xFF)
            val rd = ((b[p + 8].toInt() and 0xFF) shl 8) or (b[p + 9].toInt() and 0xFF)
            p += 10
            if (type == 1 && rd == 4 && p + 4 <= len) {
                ips.add("${b[p].toInt() and 0xFF}.${b[p + 1].toInt() and 0xFF}.${b[p + 2].toInt() and 0xFF}.${b[p + 3].toInt() and 0xFF}")
            }
            p += rd
        }
        return ips
    }
}
