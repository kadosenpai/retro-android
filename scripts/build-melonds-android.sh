#!/usr/bin/env bash
# Biên dịch lõi melonDS DS (Nintendo DS/DSi, libretro, của JesseTG) cho Android arm64 bằng NDK, đặt vào app/src/main/jniLibs.
# Lõi build bằng CMake và tự tải các thư viện phụ (melonDS gốc, libslirp, fmt, zlib...) từ GitHub lúc cấu hình, nên cần mạng.
#   Biến môi trường: ANDROID_NDK_HOME, MELONDS_REF (tag/commit; mặc định v1.4.0)
set -euo pipefail
source "$(dirname "$0")/common.sh"
rc_check_ndk

MELONDS_REF="${MELONDS_REF:-v1.4.0}"
rc_fetch melondsds https://github.com/JesseTG/melonds-ds.git "$MELONDS_REF"
BUILD="$ROOT/.melondsds-build-$ABI"

# Bản phát hành Android chính thức của lõi tắt OpenGL (xem melonds-ds#23), ta chỉ dùng bộ vẽ phần mềm.
cmake -S "$SRC" -B "$BUILD" -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="$ABI" -DANDROID_PLATFORM="android-24" \
  -DCMAKE_BUILD_TYPE=Release \
  -DENABLE_OGLRENDERER=OFF -DENABLE_OPENGL=OFF \
  -DBUILD_TESTING=OFF

cmake --build "$BUILD" --target melondsds_libretro -j"$(nproc)"

SO="$(find "$BUILD" -maxdepth 3 -name 'melondsds_libretro*.so' | head -n1)"
[ -n "$SO" ] || { echo "Không thấy file lõi melonDS DS sau khi build" >&2; exit 1; }
# Đặt tên libmelonds_libretro.so để app nạp như trước.
rc_install melonds "$SO" "$MELONDS_REF (${COMMIT:0:8})"
