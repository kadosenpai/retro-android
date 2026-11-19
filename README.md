# Retro (Android)

Trình chơi game retro cho Android: GBA, Game Boy, Game Boy Color, NES, SNES và Nintendo DS. Giao diện Kotlin (Jetpack Compose) + lõi libretro qua NDK. Chỉ dùng ROM bạn sở hữu hợp pháp.

| Hệ máy | Lõi | Định dạng ROM |
| --- | --- | --- |
| GBA, Game Boy, Game Boy Color | mGBA 0.10.5 | `.gba` `.gb` `.gbc` |
| NES | FCEUmm | `.nes` |
| SNES | Snes9x | `.sfc` `.smc` `.swc` `.fig` |
| Nintendo DS | melonDS | `.nds` `.dsi` `.ids` |

ROM nén `.zip` chưa được hỗ trợ.

## Lấy file APK (không cần cài gì trên máy)

1. Đưa thư mục này lên GitHub (kho riêng, ví dụ `retro-android`).
2. Vào tab **Actions**, chạy workflow **Build APK** (tự chạy mỗi lần push vào `main`). Workflow tự biên dịch các lõi giả lập.
3. Khi xong, vào lần chạy đó, mục **Artifacts**, tải **Retro-apk** (giải nén ra `Retro.apk`).
4. Chép `Retro.apk` sang điện thoại và cài (bật "Cài ứng dụng từ nguồn không xác định").

## Tính năng

**Thư viện**
- Mỗi hệ máy một trang (vuốt để chuyển), ảnh bìa tự tìm (theo mã băm trong DAT, sau đó theo tên gần đúng), tìm kiếm và sắp xếp.
- Thêm game bằng nút **+** (chọn file ROM). Khi đã có game, nút này đổi thành **quét lại thư mục game**: chọn thư mục game một lần trong Cài đặt, mỗi lần bấm quét app chép các ROM mới vào kho riêng của app (quét cả thư mục con, bỏ qua game đã có).
- Giữ lâu vào một game để đổi tên, đổi ảnh bìa, xuất/nhập file `.sav`, xoá.

**Chơi game**
- Lưu trạng thái nhiều ô, tự lưu khi thoát, tiếp tục từ lần chơi trước.
- Tay cầm ảo theo skin kiểu Delta, hỗ trợ tay cầm thật và gán lại nút, rung phản hồi, chế độ giữ nút.
- Bấm **menu** trên skin để mở menu; **giữ nút menu** để thoát thẳng về thư viện.
- Hiện thông số (fps, thời gian xử lý của lõi, giờ, pin) nằm trong khung của skin.
- Cheat cho GBA (GameShark/Action Replay), GB/GBC (GameShark, Game Genie), NES (Game Genie, mã thô), SNES (Game Genie, Pro Action Replay) và NDS (Action Replay); tắt cheat có hiệu lực ngay, mã sai loại sẽ báo.
- Game Boy/GBC: chọn bảng màu và viền Super Game Boy (áp dụng khi mở lại game).
- Nintendo DS: cảm ứng màn dưới; skin dọc xếp hai màn trên/dưới, skin ngang có màn lớn và màn nhỏ, chạm vào màn nhỏ để đổi chỗ hai màn.

**Skin** (đều của Stanley Sy, starvingartist; ảnh thuộc về tác giả)
- GBA, GB, GBC, NES: Atomic Advance – SmokeGray.
- SNES: SNES Pocket (JoyCon).
- NDS: Atomic NDS 2 (Onyx Black).

## Cấu trúc

- `app/src/main/cpp`: máy chủ libretro (C++), nạp lõi bằng `dlopen`, cầu nối JNI, nhận cảm ứng cho NDS.
- `scripts/build-mgba-android.sh`: biên dịch mGBA libretro cho arm64 bằng NDK.
- `scripts/build-fceumm-android.sh`, `scripts/build-snes9x-android.sh`, `scripts/build-melonds-android.sh`: biên dịch lõi FCEUmm, Snes9x, melonDS bằng ndk-build (có vá nhỏ cho melonDS khi build Android).
- `scripts/patch-mgba-cheats.py`: vá mGBA để tắt cheat gỡ được bản vá ROM (chạy tự động trong script mGBA).
- `app/src/main/java/com/retro/app`: giao diện + động cơ (Kotlin). `data/` lưu thư viện, hệ máy, cheat; `core/` nối với lõi; `render/` vẽ khung hình (một màn hình hoặc hai màn hình NDS); `ui/` các màn hình và skin.
- `app/src/main/assets/skin`: ảnh skin và ảnh nút.

## Giấy phép các lõi

mGBA: MPL 2.0. FCEUmm: GPL 2.0. melonDS: GPL 3.0. Snes9x: giấy phép phi thương mại riêng của Snes9x (dùng cá nhân thì được, không dùng bán lại). Trang Giới thiệu trong app có liên kết tới từng lõi.
