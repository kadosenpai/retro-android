#!/usr/bin/env bash
# Biên dịch lõi FCEUmm (NES, libretro) cho Android arm64 bằng NDK, đặt vào app/src/main/jniLibs.
#   Biến môi trường: ANDROID_NDK_HOME, FCEUMM_REF (nhánh/tag/commit; mặc định là commit đã thử)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK_VERSION="${NDK_VERSION:-27.1.12297006}"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/ndk/${NDK_VERSION}}}"
FCEUMM_REF="${FCEUMM_REF:-77468e568d9fe9c48d684ca911f1a3fe7259fdf7}"
ABI="${ABI:-arm64-v8a}"
API="${API:-26}"
SRC="$ROOT/.fceumm-src"
OUT="$ROOT/app/src/main/jniLibs/$ABI"

if [ ! -x "$NDK/ndk-build" ]; then
  echo "Không tìm thấy Android NDK tại: $NDK" >&2
  exit 1
fi

if [ ! -d "$SRC/.git" ]; then
  mkdir -p "$SRC"
  git -C "$SRC" init -q
  git -C "$SRC" remote add origin https://github.com/libretro/libretro-fceumm.git
fi
git -C "$SRC" fetch -q --depth 1 origin "$FCEUMM_REF"
git -C "$SRC" checkout -q --force FETCH_HEAD
COMMIT="$(git -C "$SRC" rev-parse HEAD)"
echo "FCEUmm commit: $COMMIT"

rm -rf "$SRC/libs" "$SRC/obj"
# jni/Android.mk của FCEUmm tạo module "retro" (libretro.so); ghi đè ABI và API trên dòng lệnh.
"$NDK/ndk-build" -C "$SRC/jni" -j"$(nproc)" APP_ABI="$ABI" APP_PLATFORM="android-$API" NDK_PROJECT_PATH="$SRC"

mkdir -p "$OUT"
cp "$SRC/libs/$ABI/libretro.so" "$OUT/libfceumm_libretro.so"
"$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$OUT/libfceumm_libretro.so" || true
mkdir -p "$ROOT/app/src/main/assets"
echo "${COMMIT:0:8}" > "$ROOT/app/src/main/assets/fceumm-version.txt"
ls -la "$OUT"
