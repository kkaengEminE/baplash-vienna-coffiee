#include "metronome.h"

#include <chrono>
#include <android/log.h>

#define LOG_TAG "Metronome"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

Metronome::Metronome(int32_t sampleRate)
    : sampleRate_(sampleRate),
      bpm_(120),
      beatsPerMeasure_(4),
      playing_(false),
      globalSampleCounter_(0),
      samplesPerBeat_(0) {
    clickDurationSamples_ = static_cast<int32_t>(CLICK_DURATION_SEC * sampleRate_);
    updateSamplesPerBeat();
}

void Metronome::setBpm(int32_t bpm) {
    bpm_.store(bpm);
    updateSamplesPerBeat();
}

void Metronome::setBeatsPerMeasure(int32_t beats) {
    beatsPerMeasure_.store(beats);
}

void Metronome::start() {
    globalSampleCounter_ = 0;
    playing_.store(true);
    LOGD("Metronome started: BPM=%d, beats=%d", bpm_.load(), beatsPerMeasure_.load());
}

void Metronome::stop() {
    playing_.store(false);
    LOGD("Metronome stopped");
}

void Metronome::renderAudio(float* buffer, int32_t numFrames) {
    if (!playing_.load()) {
        // Fill with silence
        for (int32_t i = 0; i < numFrames; i++) {
            buffer[i] = 0.0f;
        }
        return;
    }

    int32_t spb = samplesPerBeat_;
    int32_t bpm = beatsPerMeasure_.load();

    for (int32_t i = 0; i < numFrames; i++) {
        int32_t posInBeat = static_cast<int32_t>(globalSampleCounter_ % spb);
        int32_t beatNumber = static_cast<int32_t>((globalSampleCounter_ / spb) % bpm);

        // Record beat timestamp at the exact start of each beat
        if (posInBeat == 0) {
            auto now = std::chrono::steady_clock::now();
            int64_t nowMs = std::chrono::duration_cast<std::chrono::milliseconds>(
                now.time_since_epoch()
            ).count();
            beatTimestamps_.push_back({nowMs, beatNumber});
        }

        // Synthesize click sound
        if (posInBeat < clickDurationSamples_) {
            float freq = (beatNumber == 0) ? DOWNBEAT_FREQ : UPBEAT_FREQ;
            float t = static_cast<float>(posInBeat) / sampleRate_;
            float envelope = expf(-DECAY_RATE * static_cast<float>(posInBeat) / clickDurationSamples_);
            buffer[i] = CLICK_VOLUME * sinf(2.0f * M_PI * freq * t) * envelope;
        } else {
            buffer[i] = 0.0f;
        }

        globalSampleCounter_++;
    }
}

std::vector<Metronome::BeatTimestamp> Metronome::getAndClearBeatTimestamps() {
    std::vector<BeatTimestamp> result;
    result.swap(beatTimestamps_);
    return result;
}

void Metronome::updateSamplesPerBeat() {
    int32_t currentBpm = bpm_.load();
    if (currentBpm > 0) {
        samplesPerBeat_ = sampleRate_ * 60 / currentBpm;
    }
}
