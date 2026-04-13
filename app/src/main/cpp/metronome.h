#pragma once

#include <cstdint>
#include <cmath>
#include <atomic>
#include <vector>

/**
 * Low-latency metronome click synthesizer.
 *
 * Generates click sounds directly in the Oboe audio output callback.
 * Uses sine wave with exponential decay envelope.
 * Downbeat (beat 1) has a higher pitch (1000 Hz) than upbeats (800 Hz).
 *
 * IMPORTANT: This runs on the same audio thread as onset detection,
 * ensuring both share the exact same sample clock (no drift).
 */
class Metronome {
public:
    struct BeatTimestamp {
        int64_t timestampMs;
        int32_t beatNumber;  // 0-indexed within measure
    };

    Metronome(int32_t sampleRate = 44100);
    ~Metronome() = default;

    void setBpm(int32_t bpm);
    void setBeatsPerMeasure(int32_t beats);
    void start();
    void stop();
    bool isPlaying() const { return playing_.load(); }

    /**
     * Render metronome audio into the output buffer.
     * Called from the Oboe output audio callback.
     * @param buffer Output audio buffer (mono float)
     * @param numFrames Number of frames to render
     */
    void renderAudio(float* buffer, int32_t numFrames);

    /**
     * Get beat timestamps generated since last call and clear the list.
     * These timestamps are on the same clock as the onset detector.
     */
    std::vector<BeatTimestamp> getAndClearBeatTimestamps();

private:
    int32_t sampleRate_;
    std::atomic<int32_t> bpm_;
    std::atomic<int32_t> beatsPerMeasure_;
    std::atomic<bool> playing_;

    int64_t globalSampleCounter_;
    int32_t samplesPerBeat_;

    // Click synthesis parameters
    static constexpr float DOWNBEAT_FREQ = 1000.0f;
    static constexpr float UPBEAT_FREQ = 800.0f;
    static constexpr float CLICK_DURATION_SEC = 0.01f; // 10ms
    static constexpr float DECAY_RATE = 5.0f;
    static constexpr float CLICK_VOLUME = 0.7f;

    int32_t clickDurationSamples_;

    // Beat timestamp collection
    std::vector<BeatTimestamp> beatTimestamps_;

    void updateSamplesPerBeat();
};
