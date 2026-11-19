#!/usr/bin/env bash
# Biên dịch lõi mGBA (libretro) cho Android arm64 bằng NDK, đặt vào app/src/main/jniLibs.
# Dùng trong CI (GitHub Actions) hoặc trên máy có Android NDK.
#   Biến môi trường: ANDROID_NDK_HOME (hoặc ANDROID_HOME + NDK_VERSION), MGBA_REF (nhánh/tag/commit, mặc định 0.10.5)
set -euo pipefail

source "$(dirname "$0")/common.sh"
rc_check_ndk
MGBA_REF="${MGBA_REF:-0.10.5}"
BUILD="$ROOT/.mgba-build-$ABI"

rc_fetch mgba https://github.com/mgba-emu/mgba.git "$MGBA_REF"
python3 "$ROOT/scripts/patch-mgba-cheats.py" "$SRC/src/platform/libretro/libretro.c"

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

# Giữ lại commit cho CI (file mgba-commit.txt) và hiện "<ref> (<commit>)" trong trang Giới thiệu
echo "$COMMIT" > "$ROOT/app/src/main/mgba-commit.txt"
rc_install mgba "$BUILD/mgba_libretro.so" "$MGBA_REF (${COMMIT:0:8})"
