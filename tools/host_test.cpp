// Kiểm thử máy chủ libretro trên máy tính với một ROM GBA tối giản tự tạo (vòng lặp vô hạn).
#include <cstdio>
#include <cstring>
#include <fstream>
#include <vector>
#include "../app/src/main/cpp/retro_host.h"

int main(int argc, char** argv) {
  if (argc < 2) return 2;
  // ROM tối giản tự tạo: dòng 0 nhảy tới 0xC0, byte 0xB2 = 0x96 là dấu nhận dạng GBA, 0xC0 là vòng lặp "b .".
  std::vector<uint8_t> rom(0x400, 0);
  auto w32 = [&](size_t off, uint32_t v) { for (int i = 0; i < 4; ++i) rom[off + i] = (v >> (8 * i)) & 0xFF; };
  w32(0x00, 0xEA00002E);
  rom[0xB2] = 0x96;  // byte cố định ở 0xB2 của tiêu đề GBA
  w32(0xC0, 0xEAFFFFFE);
  std::memcpy(&rom[0xA0], "TESTROM", 7);
  { std::ofstream f("/tmp/test.gba", std::ios::binary); f.write((char*)rom.data(), rom.size()); }

  std::string err = retro::InitCore(argv[1], "/tmp", "/tmp");
  if (!err.empty()) { std::printf("InitCore FAIL: %s\n", err.c_str()); return 1; }
  err = retro::LoadGame("/tmp/test.gba");
  if (!err.empty()) { std::printf("LoadGame FAIL: %s\n", err.c_str()); return 1; }
  auto av = retro::GetAvInfo();
  std::printf("av: %dx%d fps=%.4f rate=%.0f fmt=%d\n", av.width, av.height, av.fps, av.sampleRate, retro::PixelFormatCode());
  retro::SetInput(1u << 8);
  size_t audio = 0;
  std::vector<int16_t> buf(4096 * 2);
  for (int i = 0; i < 120; ++i) { retro::RunFrame(); audio += retro::DrainAudio(buf.data(), 4096); }
  std::vector<uint8_t> frame(240 * 160 * 4);
  int w, h, fmt;
  uint32_t serial = retro::CopyFrame(frame.data(), frame.size(), &w, &h, &fmt);
  std::printf("frame: %dx%d fmt=%d serial=%u audioFrames=%zu\n", w, h, fmt, serial, audio);
  std::vector<uint8_t> st;
  bool ok = retro::Serialize(&st);
  std::printf("serialize ok=%d size=%zu\n", ok, st.size());
  bool ok2 = ok && retro::Unserialize(st.data(), st.size());
  std::printf("unserialize ok=%d\n", ok2);
  std::vector<uint8_t> sram;
  std::printf("sram has=%d\n", retro::GetSram(&sram));
  retro::CheatReset();
  retro::Reset();
  retro::RunFrame();
  retro::Shutdown();
  std::printf("shutdown ok\n");
  bool pass = w == 240 && h == 160 && serial > 100 && audio > 90000 && audio < 102000 && ok && ok2;
  std::printf(pass ? "PASS\n" : "FAIL\n");
  return pass ? 0 : 1;
}
