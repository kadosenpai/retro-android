#!/usr/bin/env python3
"""Vá lõi mGBA (libretro.c) để app điều khiển cheat chính xác hơn.

1. retro_cheat_reset: trước khi xoá, tắt từng bộ cheat và gỡ bản vá ROM / hook
   (bản gốc chỉ xoá danh sách nên cheat đã tắt vẫn còn tác dụng).
2. retro_cheat_set: ghi nhận mã mà lõi không đọc được; app lấy kết quả qua
   hàm xuất khẩu mgba_host_cheat_failed().
Chạy lại nhiều lần cũng an toàn. Thiếu mẫu cần vá thì báo lỗi để CI đỏ, không vá âm thầm.
"""
import sys

path = sys.argv[1]
s = open(path, encoding="utf-8").read()
MARK = "/* retro-host patch */"
if MARK in s:
    print("libretro.c đã được vá")
    sys.exit(0)

old_reset = """void retro_cheat_reset(void) {
	mCheatDeviceClear(core->cheatDevice(core));
}
"""
new_reset = MARK + """
static int hostCheatFailed = 0;

__attribute__((visibility("default"))) int mgba_host_cheat_failed(void) {
	int failed = hostCheatFailed;
	hostCheatFailed = 0;
	return failed;
}

void retro_cheat_reset(void) {
	struct mCheatDevice* device = core->cheatDevice(core);
	size_t i;
	for (i = 0; i < mCheatSetsSize(&device->cheats); ++i) {
		struct mCheatSet* set = *mCheatSetsGetPointer(&device->cheats, i);
		set->enabled = false;
		mCheatRefresh(device, set);
		if (set->remove) {
			set->remove(set, device);
		}
	}
	mCheatDeviceClear(device);
}
"""
if old_reset not in s:
    sys.exit("Không tìm thấy retro_cheat_reset để vá")
s = s.replace(old_reset, new_reset, 1)

call = "mCheatAddLine(cheatSet, realCode, 0);"
if s.count(call) != 2:
    sys.exit("Số lần gọi mCheatAddLine không như dự kiến: %d" % s.count(call))
s = s.replace(call, "if (!mCheatAddLine(cheatSet, realCode, 0)) { hostCheatFailed = 1; }")

open(path, "w", encoding="utf-8").write(s)
print("Đã vá libretro.c")
