#!/usr/bin/env bash
# Biên dịch lõi Snes9x (SNES, libretro) cho Android arm64 bằng NDK, đặt vào app/src/main/jniLibs.
#   Biến môi trường: ANDROID_NDK_HOME, SNES9X_REF (nhánh/tag/commit; mặc định là commit đã thử)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK_VERSION="${NDK_VERSION:-27.1.12297006}"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/ndk/${NDK_VERSION}}}"
SNES9X_REF="${SNES9X_REF:-204ea1c0b851f960cbf223075eb1c25641cb483c}"
ABI="${ABI:-arm64-v8a}"
API="${API:-26}"
SRC="$ROOT/.snes9x-src"
OUT="$ROOT/app/src/main/jniLibs/$ABI"

if [ ! -x "$NDK/ndk-build" ]; then
  echo "Không tìm thấy Android NDK tại: $NDK" >&2
  exit 1
fi

if [ ! -d "$SRC/.git" ]; then
  mkdir -p "$SRC"
  git -C "$SRC" init -q
  git -C "$SRC" remote add origin https://github.com/libretro/snes9x.git
fi
git -C "$SRC" fetch -q --depth 1 origin "$SNES9X_REF"
git -C "$SRC" checkout -q --force FETCH_HEAD
COMMIT="$(git -C "$SRC" rev-parse HEAD)"
echo "Snes9x commit: $COMMIT"

rm -rf "$SRC/libretro/libs" "$SRC/libretro/obj"
# libretro/jni/Android.mk của Snes9x tạo module "retro" (libretro.so); ghi đè ABI và API trên dòng lệnh.
"$NDK/ndk-build" -C "$SRC/libretro/jni" -j"$(nproc)" APP_ABI="$ABI" APP_PLATFORM="android-$API" NDK_PROJECT_PATH="$SRC/libretro" APP_BUILD_SCRIPT="$SRC/libretro/jni/Android.mk" NDK_APPLICATION_MK="$SRC/libretro/jni/Application.mk"

mkdir -p "$OUT"
cp "$SRC/libretro/libs/$ABI/libretro.so" "$OUT/libsnes9x_libretro.so"
"$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$OUT/libsnes9x_libretro.so" || true
mkdir -p "$ROOT/app/src/main/assets"
echo "${COMMIT:0:8}" > "$ROOT/app/src/main/assets/snes9x-version.txt"
ls -la "$OUT"
