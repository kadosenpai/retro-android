cmake_minimum_required(VERSION 3.22.1)
project(retro_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Lõi mGBA (libmgba_libretro.so) được nạp lúc chạy bằng dlopen, nên không cần liên kết ở đây.
add_library(retro_host SHARED retro_host.cpp jni_bridge.cpp gen3_text.cpp)
target_compile_options(retro_host PRIVATE -O2 -Wall -fvisibility=hidden)
target_link_libraries(retro_host log dl)
