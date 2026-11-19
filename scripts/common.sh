#!/usr/bin/env bash
# Hàm dùng chung cho các script build lõi libretro (dùng bằng: source "$(dirname "$0")/common.sh").
# Biến môi trường: ANDROID_NDK_HOME (hoặc ANDROID_HOME + NDK_VERSION), ABI, API.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NDK_VERSION="${NDK_VERSION:-27.1.12297006}"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/ndk/${NDK_VERSION}}}"
ABI="${ABI:-arm64-v8a}"
API="${API:-26}"
OUT="$ROOT/app/src/main/jniLibs/$ABI"
STRIP="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"

# rc_check_ndk: dừng nếu không có NDK.
rc_check_ndk() {
  if [ ! -f "$NDK/build/cmake/android.toolchain.cmake" ]; then
    echo "Không tìm thấy Android NDK tại: $NDK" >&2
    exit 1
  fi
}

# rc_fetch <tên> <url git> <nhánh/tag/commit>: tải đúng một commit vào $SRC (=.<tên>-src), đặt $COMMIT.
rc_fetch() {
  local name="$1" url="$2" ref="$3"
  SRC="$ROOT/.$name-src"
  if [ ! -d "$SRC/.git" ]; then
    mkdir -p "$SRC"
    git -C "$SRC" init -q
    git -C "$SRC" remote add origin "$url"
  fi
  git -C "$SRC" fetch -q --depth 1 origin "$ref"
  git -C "$SRC" checkout -q --force FETCH_HEAD
  COMMIT="$(git -C "$SRC" rev-parse HEAD)"
  echo "$name commit: $COMMIT"
}

# rc_ndk_build <thư mục jni chứa Android.mk> [thư mục dự án NDK, mặc định là thư mục cha của jni]
# Android.mk của các lõi libretro đều tạo module "retro" => $proj/libs/$ABI/libretro.so; ABI/API ghi đè trên dòng lệnh.
rc_ndk_build() {
  local jni="$1" proj="${2:-$(dirname "$1")}"
  rm -rf "$proj/libs" "$proj/obj"
  local extra=(APP_BUILD_SCRIPT="$jni/Android.mk")
  [ -f "$jni/Application.mk" ] && extra+=(NDK_APPLICATION_MK="$jni/Application.mk")
  "$NDK/ndk-build" -C "$jni" -j"$(nproc)" APP_ABI="$ABI" APP_PLATFORM="android-$API" \
    NDK_PROJECT_PATH="$proj" "${extra[@]}"
  BUILT_SO="$proj/libs/$ABI/libretro.so"
}

# rc_install <id lõi> <file .so đã build> [chuỗi phiên bản; mặc định 8 ký tự đầu của $COMMIT]
# => jniLibs/lib<id>_libretro.so và assets/<id>-version.txt (hiện trong trang Giới thiệu).
rc_install() {
  local id="$1" so="$2" ver="${3:-${COMMIT:0:8}}"
  mkdir -p "$OUT" "$ROOT/app/src/main/assets"
  cp "$so" "$OUT/lib${id}_libretro.so"
  "$STRIP" --strip-unneeded "$OUT/lib${id}_libretro.so" || true
  echo "$ver" > "$ROOT/app/src/main/assets/${id}-version.txt"
  ls -la "$OUT"
}
