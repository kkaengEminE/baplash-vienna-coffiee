#include <jni.h>
#include <memory>
#include <android/log.h>

#include "audio_engine.h"

#define LOG_TAG "JNI_Bridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::unique_ptr<AudioEngine> gEngine;

static AudioEngine* getEngine() {
    if (!gEngine) {
        gEngine = std::make_unique<AudioEngine>();
    }
    return gEngine.get();
}

extern "C" {

JNIEXPORT void JNICALL
Java_coffee_vienna_baplash_audio_AudioEngineBinding_nativeStartCapture(JNIEnv*, jobject) {
    getEngine()->startCapture();
}

JNIEXPORT void JNICALL
Java_coffee_vienna_baplash_audio_AudioEngineBinding_nativeStopCapture(JNIEnv*, jobject) {
    getEngine()->stopCapture();
}

JNIEXPORT void JNICALL
Java_coffee_vienna_baplash_audio_AudioEngineBinding_nativeStartMetronome(
    JNIEnv*, jobject, jint bpm, jint beatsPerMeasure
) {
    getEngine()->startMetronome(bpm, beatsPerMeasure);
}

JNIEXPORT void JNICALL
Java_coffee_vienna_baplash_audio_AudioEngineBinding_nativeStopMetronome(JNIEnv*, jobject) {
    getEngine()->stopMetronome();
}

JNIEXPORT void JNICALL
Java_coffee_vienna_baplash_audio_AudioEngineBinding_nativeSetBpm(JNIEnv*, jobject, jint bpm) {
    getEngine()->setBpm(bpm);
}

JNIEXPORT jfloatArray JNICALL
Java_coffee_vienna_baplash_audio_AudioEngineBinding_nativeGetOnsets(JNIEnv* env, jobject) {
    auto onsets = getEngine()->getOnsets();

    // Pack as [timestamp1, amplitude1, timestamp2, amplitude2, ...]
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(onsets.size() * 2));
    if (result == nullptr) return nullptr;

    std::vector<float> packed;
    packed.reserve(onsets.size() * 2);
    for (const auto& [timestamp, amplitude] : onsets) {
        packed.push_back(timestamp);
        packed.push_back(amplitude);
    }

    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(packed.size()), packed.data());
    return result;
}

JNIEXPORT jint JNICALL
Java_coffee_vienna_baplash_audio_AudioEngineBinding_nativeGetLatencyMs(JNIEnv*, jobject) {
    return getEngine()->getLatencyMs();
}

JNIEXPORT void JNICALL
Java_coffee_vienna_baplash_audio_AudioEngineBinding_nativeSetLatencyMs(JNIEnv*, jobject, jint ms) {
    getEngine()->setLatencyMs(ms);
}

JNIEXPORT void JNICALL
Java_coffee_vienna_baplash_audio_AudioEngineBinding_nativeDestroy(JNIEnv*, jobject) {
    if (gEngine) {
        gEngine->destroy();
        gEngine.reset();
    }
}

} // extern "C"
