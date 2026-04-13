cmake_minimum_required(VERSION 3.22.1)
project("rhythmtrainer_audio" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Oboe package
find_package(oboe REQUIRED CONFIG)

# KissFFT sources (header-only usage)
set(KISSFFT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/kissfft)

add_library(${PROJECT_NAME} SHARED
    jni_bridge.cpp
    audio_engine.cpp
    onset_detector.cpp
    metronome.cpp
    ring_buffer.cpp
    latency_calibrator.cpp
)

target_include_directories(${PROJECT_NAME} PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${KISSFFT_DIR}
)

target_link_libraries(${PROJECT_NAME}
    oboe::oboe
    android
    log
)
