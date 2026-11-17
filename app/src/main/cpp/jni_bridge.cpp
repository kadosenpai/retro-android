// Cầu nối JNI cho lớp Kotlin com.retro.app.core.NativeCore.
#include <jni.h>

#include <string>
#include <vector>

#include "retro_host.h"

namespace {

std::string Str(JNIEnv* env, jstring s) {
  if (!s) return "";
  const char* c = env->GetStringUTFChars(s, nullptr);
  std::string out(c ? c : "");
  if (c) env->ReleaseStringUTFChars(s, c);
  return out;
}

jstring NullIfEmpty(JNIEnv* env, const std::string& s) {
  return s.empty() ? nullptr : env->NewStringUTF(s.c_str());
}

jbyteArray ToByteArray(JNIEnv* env, const std::vector<uint8_t>& v) {
  jbyteArray arr = env->NewByteArray(static_cast<jsize>(v.size()));
  if (arr) env->SetByteArrayRegion(arr, 0, static_cast<jsize>(v.size()), reinterpret_cast<const jbyte*>(v.data()));
  return arr;
}

}  // namespace

extern "C" {

#define JNI(ret, name) JNIEXPORT ret JNICALL Java_com_retro_app_core_NativeCore_##name

// Trả về null nếu thành công, ngược lại là thông báo lỗi.
JNI(jstring, nativeInit)(JNIEnv* env, jobject, jstring core, jstring sys, jstring save) {
  return NullIfEmpty(env, retro::InitCore(Str(env, core), Str(env, sys), Str(env, save)));
}

JNI(jstring, nativeLoadGame)(JNIEnv* env, jobject, jstring rom) {
  return NullIfEmpty(env, retro::LoadGame(Str(env, rom)));
}

JNI(void, nativeUnloadGame)(JNIEnv*, jobject) { retro::UnloadGame(); }
JNI(void, nativeRunFrame)(JNIEnv*, jobject) { retro::RunFrame(); }
JNI(void, nativeReset)(JNIEnv*, jobject) { retro::Reset(); }
JNI(void, nativeSetInput)(JNIEnv*, jobject, jint mask) { retro::SetInput(static_cast<uint32_t>(mask)); }

// [fps, sampleRate, width, height, pixelFormat]
JNI(jdoubleArray, nativeAvInfo)(JNIEnv* env, jobject) {
  retro::AvInfo av = retro::GetAvInfo();
  jdouble v[5] = {av.fps, av.sampleRate, static_cast<jdouble>(av.width), static_cast<jdouble>(av.height),
                  static_cast<jdouble>(retro::PixelFormatCode())};
  jdoubleArray arr = env->NewDoubleArray(5);
  if (arr) env->SetDoubleArrayRegion(arr, 0, 5, v);
  return arr;
}

// Sao chép khung hình vào ByteBuffer trực tiếp. info = [w, h, fmt]. Trả về số thứ tự khung (-1 nếu lỗi).
JNI(jlong, nativeCopyFrame)(JNIEnv* env, jobject, jobject buffer, jintArray info) {
  auto* dst = static_cast<uint8_t*>(env->GetDirectBufferAddress(buffer));
  jlong cap = env->GetDirectBufferCapacity(buffer);
  if (!dst || cap <= 0) return -1;
  int w = 0, h = 0, fmt = 0;
  uint32_t serial = retro::CopyFrame(dst, static_cast<size_t>(cap), &w, &h, &fmt);
  if (info) {
    jint v[3] = {w, h, fmt};
    env->SetIntArrayRegion(info, 0, 3, v);
  }
  return serial == 0xFFFFFFFFu ? -1 : static_cast<jlong>(serial);
}

// Điền tối đa out.size/2 khung (cặp L,R) và trả về số khung đã điền.
JNI(jint, nativeDrainAudio)(JNIEnv* env, jobject, jshortArray out) {
  jsize len = env->GetArrayLength(out);
  std::vector<int16_t> tmp(static_cast<size_t>(len));
  size_t frames = retro::DrainAudio(tmp.data(), static_cast<size_t>(len) / 2);
  if (frames) env->SetShortArrayRegion(out, 0, static_cast<jsize>(frames * 2), tmp.data());
  return static_cast<jint>(frames);
}

JNI(jbyteArray, nativeGetSram)(JNIEnv* env, jobject) {
  std::vector<uint8_t> v;
  if (!retro::GetSram(&v)) return nullptr;
  return ToByteArray(env, v);
}

JNI(void, nativeSetSram)(JNIEnv* env, jobject, jbyteArray data) {
  jsize n = env->GetArrayLength(data);
  std::vector<uint8_t> v(static_cast<size_t>(n));
  env->GetByteArrayRegion(data, 0, n, reinterpret_cast<jbyte*>(v.data()));
  retro::SetSram(v.data(), v.size());
}

JNI(jbyteArray, nativeSerialize)(JNIEnv* env, jobject) {
  std::vector<uint8_t> v;
  if (!retro::Serialize(&v)) return nullptr;
  return ToByteArray(env, v);
}

JNI(jboolean, nativeUnserialize)(JNIEnv* env, jobject, jbyteArray data) {
  jsize n = env->GetArrayLength(data);
  std::vector<uint8_t> v(static_cast<size_t>(n));
  env->GetByteArrayRegion(data, 0, n, reinterpret_cast<jbyte*>(v.data()));
  return retro::Unserialize(v.data(), v.size()) ? JNI_TRUE : JNI_FALSE;
}

JNI(void, nativeSetOption)(JNIEnv* env, jobject, jstring key, jstring value) {
  retro::SetOption(Str(env, key), Str(env, value));
}

JNI(void, nativeCheatReset)(JNIEnv*, jobject) { retro::CheatReset(); }

JNI(jboolean, nativeCheatSet)(JNIEnv* env, jobject, jint index, jboolean enabled, jstring code) {
  return retro::CheatSet(static_cast<unsigned>(index), enabled == JNI_TRUE, Str(env, code)) ? JNI_TRUE : JNI_FALSE;
}

JNI(jboolean, nativeCheatsPatched)(JNIEnv*, jobject) { return retro::CheatsPatched() ? JNI_TRUE : JNI_FALSE; }

}  // extern "C"
