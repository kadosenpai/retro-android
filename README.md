# Retro (Android)

Ứng dụng giả lập game cầm tay cho Android, giao diện Kotlin (Jetpack Compose) + lõi mGBA (C) qua NDK.
Bám sát bản web Retro. Chỉ dùng ROM bạn sở hữu hợp pháp.

## Lấy file APK (không cần cài gì trên máy)

1. Đưa thư mục này lên GitHub (kho riêng, ví dụ `retro-android`).
2. Vào tab **Actions**, chạy workflow **Build APK** (tự chạy mỗi lần push vào `main`).
3. Khi xong, vào lần chạy đó, mục **Artifacts**, tải **Retro-apk** (giải nén ra `Retro.apk`).
4. Chép `Retro.apk` sang điện thoại và cài (bật "Cài ứng dụng từ nguồn không xác định").

## Cấu trúc

- `app/src/main/cpp`: máy chủ libretro (C++), nạp lõi bằng `dlopen`, cầu nối JNI.
- `scripts/build-mgba-android.sh`: biên dịch mGBA libretro cho arm64 bằng NDK.
- `app/src/main/java/com/retro/app`: giao diện + động cơ (Kotlin).
