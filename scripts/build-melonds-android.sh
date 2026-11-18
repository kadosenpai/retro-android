#!/usr/bin/env bash
# Biên dịch lõi melonDS (Nintendo DS, libretro) cho Android arm64 bằng NDK, đặt vào app/src/main/jniLibs.
#   Biến môi trường: ANDROID_NDK_HOME, MELONDS_REF (nhánh/tag/commit; mặc định là commit đã thử)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK_VERSION="${NDK_VERSION:-27.1.12297006}"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/ndk/${NDK_VERSION}}}"
MELONDS_REF="${MELONDS_REF:-4081e8358e66f1ea11879560be6b813dfeaab75a}"
ABI="${ABI:-arm64-v8a}"
API="${API:-26}"
SRC="$ROOT/.melonds-src"
OUT="$ROOT/app/src/main/jniLibs/$ABI"

if [ ! -x "$NDK/ndk-build" ]; then
  echo "Không tìm thấy Android NDK tại: $NDK" >&2
  exit 1
fi

if [ ! -d "$SRC/.git" ]; then
  mkdir -p "$SRC"
  git -C "$SRC" init -q
  git -C "$SRC" remote add origin https://github.com/libretro/melonDS.git
fi
git -C "$SRC" fetch -q --depth 1 origin "$MELONDS_REF"
git -C "$SRC" checkout -q --force FETCH_HEAD
COMMIT="$(git -C "$SRC" rev-parse HEAD)"
echo "melonDS commit: $COMMIT"

# Android (bionic, bật _GNU_SOURCE) dùng bản strerror_r kiểu GNU trả về con trỏ; mã gốc chỉ nhận ra glibc.
sed -i 's/^#if defined(__GLIBC__) && (_GNU_SOURCE.*$/#if 1/' "$SRC/src/dolphin/CommonFuncs.cpp"
grep -n '^#if 1' "$SRC/src/dolphin/CommonFuncs.cpp" || { echo "Không vá được CommonFuncs.cpp"; exit 1; }

rm -rf "$SRC/libs" "$SRC/obj"
# jni/Android.mk của melonDS tạo module "retro" (libretro.so); ghi đè ABI và API trên dòng lệnh.
"$NDK/ndk-build" -C "$SRC/jni" -j"$(nproc)" APP_ABI="$ABI" APP_PLATFORM="android-$API" NDK_PROJECT_PATH="$SRC" APP_BUILD_SCRIPT="$SRC/jni/Android.mk" NDK_APPLICATION_MK="$SRC/jni/Application.mk"

mkdir -p "$OUT"
cp "$SRC/libs/$ABI/libretro.so" "$OUT/libmelonds_libretro.so"
"$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$OUT/libmelonds_libretro.so" || true
mkdir -p "$ROOT/app/src/main/assets"
echo "${COMMIT:0:8}" > "$ROOT/app/src/main/assets/melonds-version.txt"
ls -la "$OUT"
