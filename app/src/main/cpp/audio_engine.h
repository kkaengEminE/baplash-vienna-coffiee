#pragma once

#include <oboe/Oboe.h>
#include <thread>
#include <atomic>
#include <mutex>

#include "ring_buffer.h"
#include "onset_detector.h"
#include "metronome.h"

/**
 * Main audio engine managing Oboe streams for input (microphone) and output (metronome).
 *
 * Architecture:
 * - Input stream (AAudio/OpenSL ES): captures mic audio, writes to ring buffer
 * - Analysis thread: reads from ring buffer, runs onset detection
 * - Output stream: renders metronome clicks
 * - Both streams share the same sample clock via steady_clock timestamps
 */
class AudioEngine : public oboe::AudioStreamDataCallback {
public:
    AudioEngine();
    ~AudioEngine();

    void startCapture();
    void stopCapture();

    void startMetronome(int32_t bpm, int32_t beatsPerMeasure);
    void stopMetronome();
    void setBpm(int32_t bpm);

    /**
     * Get recent onset events (thread-safe).
     * Returns pairs of (timestampMs, amplitude).
     */
    std::vector<std::pair<float, float>> getOnsets();

    int32_t getLatencyMs() const { return latencyOffsetMs_.load(); }
    void setLatencyMs(int32_t ms) { latencyOffsetMs_.store(ms); }

    void destroy();

    // Oboe callback
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* stream,
        void* audioData,
        int32_t numFrames
    ) override;

private:
    void startAnalysisThread();
    void stopAnalysisThread();
    void analysisThreadFunc();

    // Audio streams
    std::shared_ptr<oboe::AudioStream> inputStream_;
    std::shared_ptr<oboe::AudioStream> outputStream_;

    // Ring buffer for input audio (producer: input callback, consumer: analysis thread)
    std::unique_ptr<RingBuffer> ringBuffer_;

    // Onset detector (runs on analysis thread)
    std::unique_ptr<OnsetDetector> onsetDetector_;

    // Metronome (renders in output callback)
    std::unique_ptr<Metronome> metronome_;

    // Analysis thread
    std::thread analysisThread_;
    std::atomic<bool> analysisRunning_;

    // Collected onsets (protected by mutex for JNI access)
    std::mutex onsetsMutex_;
    std::vector<std::pair<float, float>> collectedOnsets_;

    // Latency compensation
    std::atomic<int32_t> latencyOffsetMs_;

    // State
    std::atomic<bool> capturing_;
    int32_t sampleRate_;

    static constexpr int32_t RING_BUFFER_SIZE = 44100 * 2; // 2 seconds
    static constexpr int32_t ANALYSIS_BUFFER_SIZE = 4096;
};
