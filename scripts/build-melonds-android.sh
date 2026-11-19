#!/usr/bin/env bash
# Biên dịch lõi melonDS (Nintendo DS, libretro) cho Android arm64 bằng NDK, đặt vào app/src/main/jniLibs.
#   Biến môi trường: ANDROID_NDK_HOME, MELONDS_REF (nhánh/tag/commit; mặc định là commit đã thử)
set -euo pipefail
source "$(dirname "$0")/common.sh"
rc_check_ndk

rc_fetch melonds https://github.com/libretro/melonDS.git "${MELONDS_REF:-4081e8358e66f1ea11879560be6b813dfeaab75a}"

# Android (bionic, bật _GNU_SOURCE) dùng bản strerror_r kiểu GNU trả về con trỏ; mã gốc chỉ nhận ra glibc.
sed -i 's/^#if defined(__GLIBC__) && (_GNU_SOURCE.*$/#if 1/' "$SRC/src/dolphin/CommonFuncs.cpp"
grep -n '^#if 1' "$SRC/src/dolphin/CommonFuncs.cpp" || { echo "Không vá được CommonFuncs.cpp"; exit 1; }

rc_ndk_build "$SRC/jni" "$SRC"
rc_install melonds "$BUILT_SO"
