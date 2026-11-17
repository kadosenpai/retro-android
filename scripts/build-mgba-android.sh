#!/usr/bin/env bash
# Biên dịch lõi mGBA (libretro) cho Android arm64 bằng NDK, đặt vào app/src/main/jniLibs.
# Dùng trong CI (GitHub Actions) hoặc trên máy có Android NDK.
#   Biến môi trường: ANDROID_NDK_HOME (hoặc ANDROID_HOME + NDK_VERSION), MGBA_REF (nhánh/tag/commit, mặc định 0.10.5)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK_VERSION="${NDK_VERSION:-27.1.12297006}"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/ndk/${NDK_VERSION}}}"
MGBA_REF="${MGBA_REF:-0.10.5}"
ABI="${ABI:-arm64-v8a}"
API="${API:-26}"
SRC="$ROOT/.mgba-src"
BUILD="$ROOT/.mgba-build-$ABI"
OUT="$ROOT/app/src/main/jniLibs/$ABI"

if [ ! -f "$NDK/build/cmake/android.toolchain.cmake" ]; then
  echo "Không tìm thấy Android NDK tại: $NDK" >&2
  exit 1
fi

if [ ! -d "$SRC/.git" ]; then
  mkdir -p "$SRC"
  git -C "$SRC" init -q
  git -C "$SRC" remote add origin https://github.com/mgba-emu/mgba.git
fi
git -C "$SRC" fetch -q --depth 1 origin "$MGBA_REF"
git -C "$SRC" checkout -q --force FETCH_HEAD
COMMIT="$(git -C "$SRC" rev-parse HEAD)"
python3 "$ROOT/scripts/patch-mgba-cheats.py" "$SRC/src/platform/libretro/libretro.c"
echo "mGBA commit: $COMMIT"

cmake -S "$SRC" -B "$BUILD" -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-$API" \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_LIBRETRO=ON -DBUILD_QT=OFF -DBUILD_SDL=OFF \
  -DBUILD_GL=OFF -DBUILD_GLES2=OFF -DBUILD_GLES3=OFF \
  -DBUILD_SHARED=OFF -DBUILD_STATIC=OFF \
  -DBUILD_PERF=OFF -DBUILD_TEST=OFF -DBUILD_EXAMPLE=OFF -DBUILD_SUITE=OFF \
  -DUSE_EPOXY=OFF -DUSE_FFMPEG=OFF -DUSE_ZLIB=OFF -DUSE_MINIZIP=OFF \
  -DUSE_PNG=OFF -DUSE_LIBZIP=OFF -DUSE_SQLITE3=OFF -DUSE_LZMA=OFF \
  -DUSE_ELF=OFF -DUSE_EDITLINE=OFF -DUSE_DISCORD_RPC=OFF \
  -DENABLE_SCRIPTING=OFF

cmake --build "$BUILD" --target mgba_libretro

mkdir -p "$OUT"
cp "$BUILD/mgba_libretro.so" "$OUT/libmgba_libretro.so"
"$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$OUT/libmgba_libretro.so" || true
echo "$COMMIT" > "$OUT/../mgba-commit.txt"
# Hiện trong trang Giới thiệu của app
mkdir -p "$ROOT/app/src/main/assets"
echo "$MGBA_REF (${COMMIT:0:8})" > "$ROOT/app/src/main/assets/mgba-version.txt"
ls -la "$OUT"
