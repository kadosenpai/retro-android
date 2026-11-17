#pragma once
// Lấy chữ tiếng Anh từ ROM Pokémon thế hệ 3 (Emerald/Ruby/Sapphire/FireRed/LeafGreen bản Mỹ).
// Chữ lưu không nén theo bảng mã riêng, mỗi đoạn kết thúc bằng byte 0xFF.
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace gen3 {

struct Entry {
  uint32_t offset;  // vị trí đoạn chữ trong ROM
  uint32_t length;  // số byte (gồm byte kết thúc 0xFF)
  std::string text; // UTF-8; \n \l \p là mã xuống dòng/cuộn/sang trang, {Pxx} là biến, {Cxx:yy} là mã điều khiển
};

// Quét ROM và trả về các đoạn chữ tiếng Anh (đã lọc bớt dữ liệu không phải chữ), theo thứ tự vị trí.
std::vector<Entry> ExtractEnglish(const uint8_t* rom, size_t size);

// Giải mã một đoạn bắt đầu tại data (tối đa max byte). Trả false nếu gặp byte không hợp lệ.
bool Decode(const uint8_t* data, size_t max, std::string* out, size_t* consumed);

}  // namespace gen3
