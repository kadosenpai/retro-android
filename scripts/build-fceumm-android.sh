#!/usr/bin/env bash
# Biên dịch lõi FCEUmm (NES, libretro) cho Android arm64 bằng NDK, đặt vào app/src/main/jniLibs.
#   Biến môi trường: ANDROID_NDK_HOME, FCEUMM_REF (nhánh/tag/commit; mặc định là commit đã thử)
set -euo pipefail
source "$(dirname "$0")/common.sh"
rc_check_ndk

rc_fetch fceumm https://github.com/libretro/libretro-fceumm.git "${FCEUMM_REF:-77468e568d9fe9c48d684ca911f1a3fe7259fdf7}"
rc_ndk_build "$SRC/jni" "$SRC"
rc_install fceumm "$BUILT_SO"
