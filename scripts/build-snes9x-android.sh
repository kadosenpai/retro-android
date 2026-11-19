#!/usr/bin/env bash
# Biên dịch lõi Snes9x (SNES, libretro) cho Android arm64 bằng NDK, đặt vào app/src/main/jniLibs.
#   Biến môi trường: ANDROID_NDK_HOME, SNES9X_REF (nhánh/tag/commit; mặc định là commit đã thử)
set -euo pipefail
source "$(dirname "$0")/common.sh"
rc_check_ndk

rc_fetch snes9x https://github.com/libretro/snes9x.git "${SNES9X_REF:-204ea1c0b851f960cbf223075eb1c25641cb483c}"
rc_ndk_build "$SRC/libretro/jni" "$SRC/libretro"
rc_install snes9x "$BUILT_SO"
