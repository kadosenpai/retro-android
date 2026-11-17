#include "gen3_text.h"

#include <cstring>

namespace gen3 {
namespace {

// Bảng mã chữ (bản tiếng Anh). nullptr = byte không phải chữ.
const char* g_char[256];
bool g_init = false;

// Số byte tham số theo mã 0xFC; -1 = mã không hợp lệ.
int FcParams(uint8_t code) {
  static const int8_t t[0x19] = {-1, 1, 1, 1, 3, 1, 1, 0, 1, 0, 1, 2, 1, 1, 1, 0, 2, 1, 1, 1, 1, 0, 0, 0, 1};
  return code < 0x19 ? t[code] : -1;
}

void Init() {
  if (g_init) return;
  g_init = true;
  memset(g_char, 0, sizeof g_char);
  g_char[0x00] = " ";
  static const char* acc1[] = {"À", "Á", "Â", "Ç", "È", "É", "Ê", "Ë", "Ì"};
  for (int i = 0; i < 9; ++i) g_char[0x01 + i] = acc1[i];
  struct P { int b; const char* c; };
  static const P ps[] = {
      {0x0B, "Î"}, {0x0C, "Ï"}, {0x0D, "Ò"}, {0x0E, "Ó"}, {0x0F, "Ô"}, {0x10, "Œ"}, {0x11, "Ù"}, {0x12, "Ú"},
      {0x13, "Û"}, {0x14, "Ñ"}, {0x15, "ß"}, {0x16, "à"}, {0x17, "á"}, {0x19, "ç"}, {0x1A, "è"}, {0x1B, "é"},
      {0x1C, "ê"}, {0x1D, "ë"}, {0x1E, "ì"}, {0x20, "î"}, {0x21, "ï"}, {0x22, "ò"}, {0x23, "ó"}, {0x24, "ô"},
      {0x25, "œ"}, {0x26, "ù"}, {0x27, "ú"}, {0x28, "û"}, {0x29, "ñ"}, {0x2A, "º"}, {0x2B, "ª"}, {0x2D, "&"},
      {0x2E, "+"}, {0x35, "="}, {0x36, ";"}, {0x51, "¿"}, {0x52, "¡"}, {0x5A, "Í"}, {0x5B, "%"}, {0x5C, "("},
      {0x5D, ")"}, {0x68, "â"}, {0x6F, "í"}, {0x79, "↑"}, {0x7A, "↓"}, {0x7B, "←"}, {0x7C, "→"}, {0x85, "<"},
      {0x86, ">"}, {0xAB, "!"}, {0xAC, "?"}, {0xAD, "."}, {0xAE, "-"}, {0xAF, "·"}, {0xB0, "…"}, {0xB1, "“"},
      {0xB2, "”"}, {0xB3, "‘"}, {0xB4, "’"}, {0xB5, "♂"}, {0xB6, "♀"}, {0xB7, "$"}, {0xB8, ","}, {0xB9, "×"},
      {0xBA, "/"}, {0xEF, "▶"}, {0xF0, ":"}, {0xF1, "Ä"}, {0xF2, "Ö"}, {0xF3, "Ü"}, {0xF4, "ä"}, {0xF5, "ö"},
      {0xF6, "ü"},
      {0x34, "{LV}"}, {0x53, "{PK}"}, {0x54, "{MN}"}, {0x55, "{PO}"}, {0x56, "{Ke}"}, {0x57, "{Bl}"},
      {0x58, "{Oc}"}, {0x59, "{K}"}, {0xFA, "\\l"}, {0xFB, "\\p"}, {0xFE, "\\n"},
  };
  for (const P& p : ps) g_char[p.b] = p.c;
  static const char* digits[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
  for (int i = 0; i < 10; ++i) g_char[0xA1 + i] = digits[i];
  static char letters[52][2];
  for (int i = 0; i < 26; ++i) {
    letters[i][0] = static_cast<char>('A' + i);
    letters[i][1] = 0;
    letters[26 + i][0] = static_cast<char>('a' + i);
    letters[26 + i][1] = 0;
    g_char[0xBB + i] = letters[i];
    g_char[0xD5 + i] = letters[26 + i];
  }
}

void AppendHex(std::string* s, unsigned v) {
  static const char* h = "0123456789ABCDEF";
  s->push_back(h[(v >> 4) & 15]);
  s->push_back(h[v & 15]);
}

// UTF-8 -> điểm mã (chỉ cần đếm và phân loại).
std::vector<char32_t> Cps(const std::string& s) {
  std::vector<char32_t> out;
  for (size_t i = 0; i < s.size();) {
    unsigned char c = static_cast<unsigned char>(s[i]);
    if (c < 0x80) { out.push_back(c); i += 1; }
    else if ((c >> 5) == 6 && i + 1 < s.size()) { out.push_back(((c & 31) << 6) | (s[i + 1] & 63)); i += 2; }
    else if ((c >> 4) == 14 && i + 2 < s.size()) { out.push_back(((c & 15) << 12) | ((s[i + 1] & 63) << 6) | (s[i + 2] & 63)); i += 3; }
    else { out.push_back(c); i += 1; }
  }
  return out;
}

bool IsAsciiLetter(char32_t c) { return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'); }
bool IsLetter(char32_t c) { return IsAsciiLetter(c) || (c >= 0xC0 && c <= 0xFF && c != 0xD7 && c != 0xF7) || c == 0x152 || c == 0x153; }
bool IsVowel(char32_t c) { return c && strchr("aeiouyAEIOUY", static_cast<int>(c)) != nullptr && c < 128; }

// Bước lọc 1: có vẻ là chữ.
bool LooksLikeText(const std::vector<char32_t>& s) {
  if (s.size() < 4) return false;
  size_t letters = 0;
  bool word = false;
  for (size_t i = 0; i < s.size(); ++i) {
    if (IsLetter(s[i])) ++letters;
    if (i + 1 < s.size() && IsAsciiLetter(s[i]) && IsAsciiLetter(s[i + 1])) word = true;
  }
  return letters >= 3 && static_cast<double>(letters) / s.size() > 0.5 && word;
}

// Bước lọc 2: loại dữ liệu rác, giữ câu tiếng Anh. Mã {…} và \l \n \p coi như khoảng trắng.
bool LooksEnglish(const std::vector<char32_t>& s) {
  std::vector<char32_t> t;
  for (size_t i = 0; i < s.size();) {
    if (s[i] == '{') {
      while (i < s.size() && s[i] != '}') ++i;
      ++i;
      t.push_back(' ');
    } else if (s[i] == '\\' && i + 1 < s.size() && (s[i + 1] == 'l' || s[i + 1] == 'n' || s[i + 1] == 'p')) {
      i += 2;
      t.push_back(' ');
    } else {
      t.push_back(s[i++]);
    }
  }
  size_t a = 0, b = t.size();
  while (a < b && (t[a] == ' ')) ++a;  // giống str.strip() cho khoảng trắng
  while (b > a && (t[b - 1] == ' ')) --b;
  if (b - a < 4) return false;
  size_t ok = 0;
  for (char32_t c : t) {
    if (IsAsciiLetter(c) || (c >= '0' && c <= '9') || c == ' ' || c == ',' || c == '.' || c == '\'' || c == '!' ||
        c == '?' || c == '-' || c == ':' || c == 0xE9 || c == 0x2019 || c == 0x2026 || c == 0x201C || c == 0x201D ||
        c == '/' || c == '&' || c == '%' || c == '$' || c == '(' || c == ')')
      ++ok;
  }
  if (static_cast<double>(ok) / t.size() < 0.95) return false;
  size_t words = 0, vowelWords = 0;
  for (size_t i = 0; i < t.size();) {
    if (!IsAsciiLetter(t[i])) { ++i; continue; }
    bool v = false;
    while (i < t.size() && IsAsciiLetter(t[i])) { v = v || IsVowel(t[i]); ++i; }
    ++words;
    if (v) ++vowelWords;
  }
  return words >= 1 && static_cast<double>(vowelWords) / words >= 0.8;
}

}  // namespace

bool Decode(const uint8_t* d, size_t max, std::string* out, size_t* consumed) {
  Init();
  out->clear();
  size_t i = 0;
  while (i < max) {
    uint8_t v = d[i];
    if (v == 0xFF) {
      *consumed = i + 1;
      return true;
    }
    if (v == 0xFC) {
      if (i + 1 >= max) return false;
      int n = FcParams(d[i + 1]);
      if (n < 0 || i + 2 + static_cast<size_t>(n) > max) return false;
      out->append("{C");
      AppendHex(out, d[i + 1]);
      for (int k = 0; k < n; ++k) {
        out->push_back(':');
        AppendHex(out, d[i + 2 + k]);
      }
      out->push_back('}');
      i += 2 + n;
      continue;
    }
    if (v == 0xFD) {
      if (i + 1 >= max) return false;
      out->append("{P");
      AppendHex(out, d[i + 1]);
      out->push_back('}');
      i += 2;
      continue;
    }
    if (g_char[v]) {
      out->append(g_char[v]);
      ++i;
      continue;
    }
    return false;
  }
  return false;
}

std::vector<Entry> ExtractEnglish(const uint8_t* rom, size_t size) {
  Init();
  std::vector<Entry> res;
  const size_t limit = size < 0x1000000 ? size : 0x1000000;
  size_t i = 0;
  std::string text;
  while (i < limit) {
    uint8_t b = rom[i];
    bool startOk = (i == 0 || rom[i - 1] == 0xFF || rom[i - 1] == 0x00) && ((b != 0x00 && g_char[b]) || b == 0xFC || b == 0xFD);
    if (startOk) {
      size_t maxLen = limit - i < 700 ? limit - i : 700;
      size_t used = 0;
      if (Decode(rom + i, maxLen, &text, &used)) {
        std::vector<char32_t> cps = Cps(text);
        if (LooksLikeText(cps) && LooksEnglish(cps)) {
          res.push_back({static_cast<uint32_t>(i), static_cast<uint32_t>(used), text});
          i += used;
          continue;
        }
      }
    }
    ++i;
  }
  return res;
}

}  // namespace gen3
