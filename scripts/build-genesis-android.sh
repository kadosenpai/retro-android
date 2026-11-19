#!/usr/bin/env bash
# Biên dịch lõi Genesis Plus GX (Mega Drive / Genesis, libretro) cho Android arm64 bằng NDK.
#   Biến môi trường: ANDROID_NDK_HOME, GENESIS_REF (nhánh/tag/commit; mặc định là commit đã thử)
set -euo pipefail
source "$(dirname "$0")/common.sh"
rc_check_ndk

rc_fetch genesis https://github.com/libretro/Genesis-Plus-GX.git "${GENESIS_REF:-b9c2316441978d4015ba30e9a95d9b0e6f8ee981}"
rc_ndk_build "$SRC/libretro/jni" "$SRC/libretro"
rc_install genesis "$BUILT_SO"
