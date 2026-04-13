#pragma once

#include <cstdint>
#include <vector>
#include <deque>
#include <cmath>
#include <algorithm>
#include <chrono>

/**
 * Spectral Flux onset detection algorithm.
 *
 * Detects note onsets (when a musician plays a note) by analyzing
 * changes in the frequency spectrum of the audio signal.
 *
 * Algorithm:
 * 1. Window the audio frame (Hann window)
 * 2. Compute FFT magnitude spectrum
 * 3. Calculate spectral flux (positive changes in magnitude)
 * 4. Apply adaptive threshold (median-based)
 * 5. Pick peaks above threshold
 * 6. Enforce minimum inter-onset interval
 */
struct OnsetDetectorConfig {
    int32_t sampleRate = 44100;
    int32_t frameSize = 1024;       // FFT window size
    int32_t hopSize = 512;          // Overlap hop
    float thresholdMultiplier = 1.5f;
    float thresholdOffset = 0.01f;
    int32_t medianWindowSize = 10;  // Frames for adaptive threshold
    int64_t minInterOnsetMs = 50;   // Minimum gap between onsets
    float highPassFreqHz = 80.0f;   // High-pass filter for bass isolation
};

class OnsetDetector {
public:
    struct OnsetEvent {
        int64_t timestampMs;  // Milliseconds since epoch
        float amplitude;       // Peak amplitude at onset
    };

    using Config = OnsetDetectorConfig;

    explicit OnsetDetector(const Config& config = Config());
    ~OnsetDetector();

    /**
     * Process a buffer of audio samples. Call this from the analysis thread.
     * @param samples Input audio samples (mono, float, -1.0 to 1.0)
     * @param numSamples Number of samples in the buffer
     */
    void process(const float* samples, int32_t numSamples);

    /**
     * Get detected onsets since last call and clear the internal list.
     */
    std::vector<OnsetEvent> getAndClearOnsets();

    void reset();

private:
    void applyHannWindow(const float* input, float* output, int32_t size);
    void computeMagnitudeSpectrum(const float* windowed, float* magnitudes);
    float computeSpectralFlux(const float* currentMag, const float* prevMag, int32_t size);
    float computeAdaptiveThreshold();
    void applyHighPassFilter(float* samples, int32_t numSamples);

    Config config_;

    // FFT buffers
    float* windowedFrame_;
    float* currentMagnitudes_;
    float* previousMagnitudes_;
    float* hannWindow_;

    // Internal accumulation buffer for partial frames
    std::vector<float> accumBuffer_;
    int32_t accumPos_;

    // Spectral flux history for adaptive threshold
    std::deque<float> fluxHistory_;

    // Previous spectral flux values for peak picking
    float prevFlux_;
    float prevPrevFlux_;

    // Onset list
    std::vector<OnsetEvent> detectedOnsets_;

    // Timing
    int64_t lastOnsetTimestampMs_;
    int64_t sampleCounter_;

    // High-pass filter state
    float hpFilterState_;
    float hpAlpha_;
};
