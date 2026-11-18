# Retro (Android)

Trình chơi game retro cho Android: GBA, Game Boy, Game Boy Color và NES. Giao diện Kotlin (Jetpack Compose) + lõi mGBA 0.10.5 (GBA/GB/GBC) và FCEUmm (NES) qua NDK. Chỉ dùng ROM bạn sở hữu hợp pháp.

## Lấy file APK (không cần cài gì trên máy)

1. Đưa thư mục này lên GitHub (kho riêng, ví dụ `retro-android`).
2. Vào tab **Actions**, chạy workflow **Build APK** (tự chạy mỗi lần push vào `main`).
3. Khi xong, vào lần chạy đó, mục **Artifacts**, tải **Retro-apk** (giải nén ra `Retro.apk`).
4. Chép `Retro.apk` sang điện thoại và cài (bật "Cài ứng dụng từ nguồn không xác định").

## Tính năng

- Thư viện theo hệ máy (GBA / GB / GBC / NES), mỗi hệ một trang, ảnh bìa tự tìm, tìm kiếm và sắp xếp.
- Lưu trạng thái nhiều ô, xuất/nhập file `.sav`, hỗ trợ tay cầm và gán lại nút.
- Cheat cho GBA (GameShark/Action Replay), GB/GBC (GameShark, Game Genie) và NES (Game Genie, mã thô); tắt cheat có hiệu lực ngay, mã sai loại sẽ báo.
- Game Boy/GBC: chọn bảng màu và viền Super Game Boy (áp dụng khi mở lại game).
- Skin tay cầm ảo dạng Delta (SmokeGray).

## Cấu trúc

- `app/src/main/cpp`: máy chủ libretro (C++), nạp lõi bằng `dlopen`, cầu nối JNI.
- `scripts/build-mgba-android.sh`: biên dịch mGBA libretro cho arm64 bằng NDK.
- `scripts/build-fceumm-android.sh`: biên dịch lõi FCEUmm (NES) bằng ndk-build.
- `scripts/patch-mgba-cheats.py`: vá mGBA để tắt cheat gỡ được bản vá ROM (chạy tự động trong script trên).
- `app/src/main/java/com/retro/app`: giao diện + động cơ (Kotlin).
