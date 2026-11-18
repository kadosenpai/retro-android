#include "retro_host.h"

#include <dlfcn.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>

#include <atomic>
#include <fstream>
#include <map>
#include <mutex>

#include "libretro.h"

#ifdef __ANDROID__
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "RetroHost", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, "RetroHost", __VA_ARGS__)
#else
#define LOGI(...) (fprintf(stderr, "[I] " __VA_ARGS__), fprintf(stderr, "\n"))
#define LOGW(...) (fprintf(stderr, "[W] " __VA_ARGS__), fprintf(stderr, "\n"))
#endif

namespace retro {
namespace {

// ---- Các hàm của lõi (lấy bằng dlsym) ----
struct CoreApi {
  void* handle = nullptr;
  void (*retro_set_environment)(retro_environment_t) = nullptr;
  void (*retro_set_video_refresh)(retro_video_refresh_t) = nullptr;
  void (*retro_set_audio_sample)(retro_audio_sample_t) = nullptr;
  void (*retro_set_audio_sample_batch)(retro_audio_sample_batch_t) = nullptr;
  void (*retro_set_input_poll)(retro_input_poll_t) = nullptr;
  void (*retro_set_input_state)(retro_input_state_t) = nullptr;
  void (*retro_init)(void) = nullptr;
  void (*retro_deinit)(void) = nullptr;
  unsigned (*retro_api_version)(void) = nullptr;
  void (*retro_get_system_info)(struct retro_system_info*) = nullptr;
  void (*retro_get_system_av_info)(struct retro_system_av_info*) = nullptr;
  void (*retro_set_controller_port_device)(unsigned, unsigned) = nullptr;
  void (*retro_reset)(void) = nullptr;
  void (*retro_run)(void) = nullptr;
  size_t (*retro_serialize_size)(void) = nullptr;
  bool (*retro_serialize)(void*, size_t) = nullptr;
  bool (*retro_unserialize)(const void*, size_t) = nullptr;
  void (*retro_cheat_reset)(void) = nullptr;
  void (*retro_cheat_set)(unsigned, bool, const char*) = nullptr;
  // Chỉ có ở lõi đã vá (scripts/patch-mgba-cheats.py): trả 1 nếu mã vừa nạp bị từ chối.
  int (*mgba_host_cheat_failed)(void) = nullptr;
  bool (*retro_load_game)(const struct retro_game_info*) = nullptr;
  void (*retro_unload_game)(void) = nullptr;
  void* (*retro_get_memory_data)(unsigned) = nullptr;
  size_t (*retro_get_memory_size)(unsigned) = nullptr;
};

CoreApi g;
bool g_initialized = false;  // retro_init đã gọi
bool g_gameLoaded = false;

// Tuỳ chọn của lõi (khoá -> giá trị) do app đặt; lõi đọc qua GET_VARIABLE.
std::map<std::string, std::string> g_options;
bool g_optionsDirty = false;
std::mutex g_optionsMu;

std::string g_systemDir;
std::string g_saveDir;
std::vector<uint8_t> g_romData;  // lõi có thể giữ con trỏ tới dữ liệu ROM, nên giữ nó sống

AvInfo g_av;
std::atomic<uint32_t> g_pad{0};
// Cảm ứng (NDS): toạ độ chuẩn hoá libretro -0x8000..0x7FFF và trạng thái chạm.
std::atomic<int> g_touchX{0};
std::atomic<int> g_touchY{0};
std::atomic<int> g_touchDown{0};
int g_fmt = kFmtRgb565;  // mặc định theo libretro là 0RGB1555, nhưng ta chỉ nhận RGB565/XRGB8888

std::mutex g_frameMu;
std::vector<uint8_t> g_frame;
int g_frameW = 0, g_frameH = 0, g_frameFmt = kFmtNone;
uint32_t g_frameSerial = 0;

std::mutex g_audioMu;
std::vector<int16_t> g_audio;  // L,R xen kẽ
double g_resPos = 0.0;      // vị trí nội suy (tính theo khung nguồn)
int16_t g_resPrevL = 0, g_resPrevR = 0;  // khung nguồn đứng trước vị trí 0
const size_t kAudioCapFrames = 48000;  // giữ tối đa ~1 giây để không phình bộ nhớ

// ---- Callback ----
void RETRO_CALLCONV LogCb(enum retro_log_level level, const char* fmt, ...) {
  char buf[512];
  va_list ap;
  va_start(ap, fmt);
  vsnprintf(buf, sizeof buf, fmt, ap);
  va_end(ap);
  if (level >= RETRO_LOG_WARN) LOGW("core: %s", buf);
  else LOGI("core: %s", buf);
}

bool RETRO_CALLCONV EnvCb(unsigned cmd, void* data) {
  switch (cmd & 0xFFFF) {
    case RETRO_ENVIRONMENT_GET_CAN_DUPE & 0xFFFF:
      *static_cast<bool*>(data) = true;
      return true;
    case RETRO_ENVIRONMENT_SET_MESSAGE & 0xFFFF:
    case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL & 0xFFFF:
    case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS & 0xFFFF:
    case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME & 0xFFFF:
    case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO & 0xFFFF:
      return true;
    case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY & 0xFFFF:
      *static_cast<const char**>(data) = g_systemDir.c_str();
      return true;
    case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY & 0xFFFF:
      *static_cast<const char**>(data) = g_saveDir.c_str();
      return true;
    case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT & 0xFFFF: {
      auto f = *static_cast<enum retro_pixel_format*>(data);
      if (f == RETRO_PIXEL_FORMAT_RGB565) {
        g_fmt = kFmtRgb565;
        return true;
      }
      if (f == RETRO_PIXEL_FORMAT_XRGB8888) {
        g_fmt = kFmtXrgb8888;
        return true;
      }
      return false;
    }
    case RETRO_ENVIRONMENT_GET_VARIABLE & 0xFFFF: {
      auto* var = static_cast<struct retro_variable*>(data);
      std::lock_guard<std::mutex> l(g_optionsMu);
      auto it = var->key ? g_options.find(var->key) : g_options.end();
      if (it == g_options.end()) return false;  // chưa đặt: lõi dùng giá trị mặc định
      var->value = it->second.c_str();
      return true;
    }
    case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE & 0xFFFF: {
      std::lock_guard<std::mutex> l(g_optionsMu);
      *static_cast<bool*>(data) = g_optionsDirty;
      g_optionsDirty = false;
      return true;
    }
    case RETRO_ENVIRONMENT_GET_LOG_INTERFACE & 0xFFFF:
      static_cast<struct retro_log_callback*>(data)->log = LogCb;
      return true;
    case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS & 0xFFFF:
      return true;
    case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION & 0xFFFF:
      *static_cast<unsigned*>(data) = 0;
      return true;
    case RETRO_ENVIRONMENT_GET_LANGUAGE & 0xFFFF:
      *static_cast<unsigned*>(data) = RETRO_LANGUAGE_ENGLISH;
      return true;
    case RETRO_ENVIRONMENT_SET_GEOMETRY & 0xFFFF: {
      auto* geo = static_cast<const struct retro_game_geometry*>(data);
      g_av.width = static_cast<int>(geo->base_width);
      g_av.height = static_cast<int>(geo->base_height);
      return true;
    }
    case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO & 0xFFFF: {
      auto* info = static_cast<const struct retro_system_av_info*>(data);
      g_av.fps = info->timing.fps;
      g_av.sampleRate = info->timing.sample_rate;
      g_av.width = static_cast<int>(info->geometry.base_width);
      g_av.height = static_cast<int>(info->geometry.base_height);
      return true;
    }
    default:
      // Mọi thứ khác (SET_VARIABLES, rumble, sensor...) không hỗ trợ.
      return false;
  }
}

void RETRO_CALLCONV VideoCb(const void* data, unsigned width, unsigned height, size_t pitch) {
  if (!data) return;  // lõi báo "khung giống khung trước"
  const size_t bpp = (g_fmt == kFmtXrgb8888) ? 4 : 2;
  const size_t rowBytes = static_cast<size_t>(width) * bpp;
  std::lock_guard<std::mutex> lock(g_frameMu);
  g_frame.resize(rowBytes * height);
  const uint8_t* src = static_cast<const uint8_t*>(data);
  for (unsigned y = 0; y < height; ++y) memcpy(g_frame.data() + y * rowBytes, src + y * pitch, rowBytes);
  g_frameW = static_cast<int>(width);
  g_frameH = static_cast<int>(height);
  g_frameFmt = g_fmt;
  ++g_frameSerial;
}

void PushAudio(const int16_t* data, size_t frames) {
  std::lock_guard<std::mutex> lock(g_audioMu);
  g_audio.insert(g_audio.end(), data, data + frames * 2);
  if (g_audio.size() > kAudioCapFrames * 2) g_audio.erase(g_audio.begin(), g_audio.end() - kAudioCapFrames * 2);
}

void RETRO_CALLCONV AudioSampleCb(int16_t l, int16_t r) {
  int16_t s[2] = {l, r};
  PushAudio(s, 1);
}

size_t RETRO_CALLCONV AudioBatchCb(const int16_t* data, size_t frames) {
  PushAudio(data, frames);
  return frames;
}

void RETRO_CALLCONV InputPollCb() {}

int16_t RETRO_CALLCONV InputStateCb(unsigned port, unsigned device, unsigned /*index*/, unsigned id) {
  if (port == 0 && device == RETRO_DEVICE_POINTER) {
    if (id == RETRO_DEVICE_ID_POINTER_X) return static_cast<int16_t>(g_touchX.load(std::memory_order_relaxed));
    if (id == RETRO_DEVICE_ID_POINTER_Y) return static_cast<int16_t>(g_touchY.load(std::memory_order_relaxed));
    if (id == RETRO_DEVICE_ID_POINTER_PRESSED) return static_cast<int16_t>(g_touchDown.load(std::memory_order_relaxed));
    return 0;
  }
  if (port != 0 || device != RETRO_DEVICE_JOYPAD) return 0;
  const uint32_t pad = g_pad.load(std::memory_order_relaxed);
  if (id == RETRO_DEVICE_ID_JOYPAD_MASK) return static_cast<int16_t>(pad & 0xFFFF);
  if (id > 15) return 0;
  return (pad >> id) & 1u;
}

template <typename T>
bool Sym(void* h, const char* name, T* out) {
  *out = reinterpret_cast<T>(dlsym(h, name));
  return *out != nullptr;
}

bool ReadFile(const std::string& path, std::vector<uint8_t>* out) {
  std::ifstream f(path, std::ios::binary | std::ios::ate);
  if (!f) return false;
  std::streamsize n = f.tellg();
  if (n <= 0) return false;
  out->resize(static_cast<size_t>(n));
  f.seekg(0);
  return static_cast<bool>(f.read(reinterpret_cast<char*>(out->data()), n));
}

}  // namespace

std::string InitCore(const std::string& corePath, const std::string& systemDir, const std::string& saveDir) {
  if (g_initialized) Shutdown();
  g_systemDir = systemDir;
  g_saveDir = saveDir;

  void* h = dlopen(corePath.c_str(), RTLD_NOW | RTLD_LOCAL);
  if (!h) h = dlopen("libmgba_libretro.so", RTLD_NOW | RTLD_LOCAL);  // dự phòng: tìm theo tên
  if (!h) return std::string("Không nạp được lõi: ") + (dlerror() ? dlerror() : "?");

  CoreApi api;
  api.handle = h;
  bool ok = Sym(h, "retro_set_environment", &api.retro_set_environment) &&
            Sym(h, "retro_set_video_refresh", &api.retro_set_video_refresh) &&
            Sym(h, "retro_set_audio_sample", &api.retro_set_audio_sample) &&
            Sym(h, "retro_set_audio_sample_batch", &api.retro_set_audio_sample_batch) &&
            Sym(h, "retro_set_input_poll", &api.retro_set_input_poll) &&
            Sym(h, "retro_set_input_state", &api.retro_set_input_state) &&
            Sym(h, "retro_init", &api.retro_init) && Sym(h, "retro_deinit", &api.retro_deinit) &&
            Sym(h, "retro_api_version", &api.retro_api_version) &&
            Sym(h, "retro_get_system_info", &api.retro_get_system_info) &&
            Sym(h, "retro_get_system_av_info", &api.retro_get_system_av_info) &&
            Sym(h, "retro_set_controller_port_device", &api.retro_set_controller_port_device) &&
            Sym(h, "retro_reset", &api.retro_reset) && Sym(h, "retro_run", &api.retro_run) &&
            Sym(h, "retro_serialize_size", &api.retro_serialize_size) &&
            Sym(h, "retro_serialize", &api.retro_serialize) &&
            Sym(h, "retro_unserialize", &api.retro_unserialize) &&
            Sym(h, "retro_cheat_reset", &api.retro_cheat_reset) &&
            Sym(h, "retro_cheat_set", &api.retro_cheat_set) &&
            Sym(h, "retro_load_game", &api.retro_load_game) &&
            Sym(h, "retro_unload_game", &api.retro_unload_game) &&
            Sym(h, "retro_get_memory_data", &api.retro_get_memory_data) &&
            Sym(h, "retro_get_memory_size", &api.retro_get_memory_size);
  Sym(h, "mgba_host_cheat_failed", &api.mgba_host_cheat_failed);  // tuỳ chọn
  if (!ok) {
    dlclose(h);
    return "Lõi thiếu hàm libretro bắt buộc";
  }
  if (api.retro_api_version() != RETRO_API_VERSION) {
    dlclose(h);
    return "Phiên bản libretro không khớp";
  }
  g = api;
  g_fmt = kFmtRgb565;
  g.retro_set_environment(EnvCb);
  g.retro_set_video_refresh(VideoCb);
  g.retro_set_audio_sample(AudioSampleCb);
  g.retro_set_audio_sample_batch(AudioBatchCb);
  g.retro_set_input_poll(InputPollCb);
  g.retro_set_input_state(InputStateCb);
  g.retro_init();
  g_initialized = true;
  return "";
}

std::string LoadGame(const std::string& romPath) {
  if (!g_initialized) return "Lõi chưa được khởi tạo";
  if (g_gameLoaded) UnloadGame();
  if (!ReadFile(romPath, &g_romData)) return "Không đọc được file ROM";

  struct retro_game_info info;
  memset(&info, 0, sizeof info);
  info.path = romPath.c_str();
  info.data = g_romData.data();
  info.size = g_romData.size();
  if (!g.retro_load_game(&info)) return "Lõi không nạp được game này";
  g_gameLoaded = true;

  struct retro_system_av_info av;
  memset(&av, 0, sizeof av);
  g.retro_get_system_av_info(&av);
  g_av.fps = av.timing.fps > 1.0 ? av.timing.fps : 60.0;
  g_av.sampleRate = av.timing.sample_rate > 1.0 ? av.timing.sample_rate : 32768.0;
  g_av.width = static_cast<int>(av.geometry.base_width);
  g_av.height = static_cast<int>(av.geometry.base_height);
  g.retro_set_controller_port_device(0, RETRO_DEVICE_JOYPAD);

  {
    std::lock_guard<std::mutex> l(g_frameMu);
    g_frame.clear();
    g_frameW = g_frameH = 0;
    g_frameFmt = kFmtNone;
    g_frameSerial = 0;
  }
  {
    std::lock_guard<std::mutex> l(g_audioMu);
    g_audio.clear();
    g_resPos = 0.0;
    g_resPrevL = g_resPrevR = 0;
  }
  g_pad = 0;
  g_touchDown = 0;
  return "";
}

void UnloadGame() {
  if (g_initialized && g_gameLoaded) g.retro_unload_game();
  g_gameLoaded = false;
  g_romData.clear();
  g_romData.shrink_to_fit();
}

void Shutdown() {
  UnloadGame();
  if (g_initialized) {
    g.retro_deinit();
    g_initialized = false;
  }
  if (g.handle) dlclose(g.handle);
  g = CoreApi();
}

void RunFrame() {
  if (g_initialized && g_gameLoaded) g.retro_run();
}

void Reset() {
  if (g_initialized && g_gameLoaded) g.retro_reset();
}

void SetInput(uint32_t mask) { g_pad.store(mask, std::memory_order_relaxed); }

void SetTouch(int x, int y, bool down) {
  g_touchX.store(x, std::memory_order_relaxed);
  g_touchY.store(y, std::memory_order_relaxed);
  g_touchDown.store(down ? 1 : 0, std::memory_order_relaxed);
}

AvInfo GetAvInfo() { return g_av; }
int PixelFormatCode() { return g_fmt; }

uint32_t CopyFrame(uint8_t* dst, size_t dstCap, int* w, int* h, int* fmt) {
  std::lock_guard<std::mutex> lock(g_frameMu);
  *w = g_frameW;
  *h = g_frameH;
  *fmt = g_frameFmt;
  if (!g_frame.empty() && g_frame.size() <= dstCap) memcpy(dst, g_frame.data(), g_frame.size());
  else if (!g_frame.empty()) return 0xFFFFFFFFu;  // bộ đệm đích quá nhỏ
  return g_frameSerial;
}

size_t DrainAudio(int16_t* out, size_t maxFrames) {
  // Đổi tần số từ tần số của lõi (32768 Hz với GBA) sang kOutputRate bằng nội suy tuyến tính.
  std::lock_guard<std::mutex> lock(g_audioMu);
  const size_t N = g_audio.size() / 2;
  if (N < 2 || maxFrames == 0) return 0;
  const double ratio = (g_av.sampleRate > 1.0 ? g_av.sampleRate : 32768.0) / static_cast<double>(kOutputRate);
  auto sample = [&](long j, int ch) -> double {
    return j < 0 ? (ch == 0 ? g_resPrevL : g_resPrevR) : g_audio[static_cast<size_t>(j) * 2 + ch];
  };
  size_t n = 0;
  double pos = g_resPos;
  while (n < maxFrames) {
    long i = static_cast<long>(pos);
    if (i + 1 >= static_cast<long>(N)) break;
    double f = pos - static_cast<double>(i);
    for (int ch = 0; ch < 2; ++ch) {
      double v = sample(i, ch) * (1.0 - f) + sample(i + 1, ch) * f;
      out[n * 2 + ch] = static_cast<int16_t>(v < -32768.0 ? -32768.0 : (v > 32767.0 ? 32767.0 : v));
    }
    ++n;
    pos += ratio;
  }
  long k = static_cast<long>(pos);
  if (k > static_cast<long>(N)) k = static_cast<long>(N);
  if (k > 0) {
    g_resPrevL = g_audio[static_cast<size_t>(k - 1) * 2];
    g_resPrevR = g_audio[static_cast<size_t>(k - 1) * 2 + 1];
    g_audio.erase(g_audio.begin(), g_audio.begin() + static_cast<long>(k * 2));
    pos -= static_cast<double>(k);
  }
  g_resPos = pos;
  return n;
}

bool GetSram(std::vector<uint8_t>* out) {
  if (!g_initialized || !g_gameLoaded) return false;
  void* p = g.retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
  size_t n = g.retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
  if (!p || n == 0) return false;
  out->assign(static_cast<uint8_t*>(p), static_cast<uint8_t*>(p) + n);
  return true;
}

void SetSram(const uint8_t* data, size_t size) {
  if (!g_initialized || !g_gameLoaded) return;
  void* p = g.retro_get_memory_data(RETRO_MEMORY_SAVE_RAM);
  size_t n = g.retro_get_memory_size(RETRO_MEMORY_SAVE_RAM);
  if (!p || n == 0) return;
  memcpy(p, data, size < n ? size : n);
}

bool Serialize(std::vector<uint8_t>* out) {
  if (!g_initialized || !g_gameLoaded) return false;
  size_t n = g.retro_serialize_size();
  if (n == 0) return false;
  out->assign(n, 0);
  return g.retro_serialize(out->data(), n);
}

bool Unserialize(const uint8_t* data, size_t size) {
  if (!g_initialized || !g_gameLoaded) return false;
  return g.retro_unserialize(data, size);
}

void SetOption(const std::string& key, const std::string& value) {
  std::lock_guard<std::mutex> l(g_optionsMu);
  auto it = g_options.find(key);
  if (it != g_options.end() && it->second == value) return;
  g_options[key] = value;
  g_optionsDirty = true;
}

void CheatReset() {
  if (g_initialized && g_gameLoaded) g.retro_cheat_reset();
}

bool CheatSet(unsigned index, bool enabled, const std::string& code) {
  if (!(g_initialized && g_gameLoaded)) return true;
  g.retro_cheat_set(index, enabled, code.c_str());
  return g.mgba_host_cheat_failed ? g.mgba_host_cheat_failed() == 0 : true;
}

bool CheatsPatched() { return g_initialized && g.mgba_host_cheat_failed != nullptr; }

}  // namespace retro
