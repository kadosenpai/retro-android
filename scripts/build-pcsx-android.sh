#!/usr/bin/env bash
# Biên dịch lõi PCSX ReARMed (PlayStation 1, libretro) cho Android arm64 bằng NDK (có dynarec ARM64).
#   Biến môi trường: ANDROID_NDK_HOME, PCSX_REF (nhánh/tag/commit; mặc định là commit đã thử)
set -euo pipefail
source "$(dirname "$0")/common.sh"
rc_check_ndk

rc_fetch pcsx https://github.com/libretro/pcsx_rearmed.git "${PCSX_REF:-7b0e8f8fadfbce3d170d195b9d9eecaabfa08c8e}"
rc_ndk_build "$SRC/jni" "$SRC"
rc_install pcsx "$BUILT_SO"
