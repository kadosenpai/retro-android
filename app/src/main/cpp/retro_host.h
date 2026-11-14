// Máy chủ libretro tối giản: nạp một lõi (.so) bằng dlopen, chạy từng khung hình,
// gom hình ảnh + âm thanh để lớp JNI / Kotlin lấy đi. Không phụ thuộc Android (để kiểm thử trên máy tính).
#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace retro {

struct AvInfo {
  double fps = 60.0;
  double sampleRate = 32768.0;
  int width = 240;
  int height = 160;
};

// Mã định dạng điểm ảnh trả cho Kotlin.
enum PixelFormat { kFmtNone = 0, kFmtXrgb8888 = 1, kFmtRgb565 = 2 };

// Nạp lõi từ đường dẫn. Trả về chuỗi rỗng nếu thành công, ngược lại là thông báo lỗi.
std::string InitCore(const std::string& corePath, const std::string& systemDir, const std::string& saveDir);
// Nạp game từ file. Trả về chuỗi rỗng nếu thành công.
std::string LoadGame(const std::string& romPath);
void UnloadGame();
void Shutdown();

void RunFrame();
void Reset();
void SetInput(uint32_t mask);

AvInfo GetAvInfo();
int PixelFormatCode();
// Sao chép khung hình mới nhất vào dst (dstCap byte). Trả về số thứ tự khung hình; w,h,fmt là kích thước hiện tại.
uint32_t CopyFrame(uint8_t* dst, size_t dstCap, int* w, int* h, int* fmt);
// Lấy tối đa maxFrames mẫu âm thanh stereo (int16, xen kẽ L,R). Trả về số khung (cặp L/R).
constexpr int kOutputRate = 48000;  // tần số phát ra (Hz)
// Lấy âm thanh đã đổi sang kOutputRate (L,R xen kẽ). Trả về số khung.
size_t DrainAudio(int16_t* out, size_t maxFrames);

bool GetSram(std::vector<uint8_t>* out);
void SetSram(const uint8_t* data, size_t size);
bool Serialize(std::vector<uint8_t>* out);
bool Unserialize(const uint8_t* data, size_t size);
void CheatReset();
void CheatSet(unsigned index, bool enabled, const std::string& code);

}  // namespace retro
