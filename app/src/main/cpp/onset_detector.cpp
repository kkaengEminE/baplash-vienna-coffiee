#include "onset_detector.h"

#include <cstring>
#include <numeric>
#include <android/log.h>

#define LOG_TAG "OnsetDetector"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Simple in-place real FFT (radix-2 DIT). For production, replace with KissFFT.
// This is a minimal implementation sufficient for onset detection.
namespace {
    void fft_real(float* data, int32_t n) {
        // Bit-reversal permutation
        int32_t j = 0;
        for (int32_t i = 0; i < n; i++) {
            if (i < j) std::swap(data[i], data[j]);
            int32_t m = n >> 1;
            while (m >= 1 && j >= m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }

        // Cooley-Tukey butterfly
        for (int32_t step = 2; step <= n; step <<= 1) {
            float angle = -2.0f * M_PI / step;
            float wr = cosf(angle);
            float wi = sinf(angle);

            for (int32_t k = 0; k < n; k += step) {
                float tr = 1.0f, ti = 0.0f;
                int32_t half = step >> 1;
                for (int32_t j2 = 0; j2 < half; j2++) {
                    int32_t idx1 = k + j2;
                    int32_t idx2 = idx1 + half;
                    float ur = data[idx1];
                    float vr = data[idx2] * tr - 0.0f * ti; // Simplified for real input
                    data[idx1] = ur + vr;
                    data[idx2] = ur - vr;
                    float newTr = tr * wr - ti * wi;
                    ti = tr * wi + ti * wr;
                    tr = newTr;
                }
            }
        }
    }
}

OnsetDetector::OnsetDetector(const Config& config)
    : config_(config),
      accumPos_(0),
      prevFlux_(0.0f),
      prevPrevFlux_(0.0f),
      lastOnsetTimestampMs_(0),
      sampleCounter_(0),
      hpFilterState_(0.0f) {

    windowedFrame_ = new float[config_.frameSize];
    currentMagnitudes_ = new float[config_.frameSize / 2 + 1];
    previousMagnitudes_ = new float[config_.frameSize / 2 + 1];
    hannWindow_ = new float[config_.frameSize];

    accumBuffer_.resize(config_.frameSize, 0.0f);

    std::memset(currentMagnitudes_, 0, (config_.frameSize / 2 + 1) * sizeof(float));
    std::memset(previousMagnitudes_, 0, (config_.frameSize / 2 + 1) * sizeof(float));

    // Pre-compute Hann window
    for (int32_t i = 0; i < config_.frameSize; i++) {
        hannWindow_[i] = 0.5f * (1.0f - cosf(2.0f * M_PI * i / (config_.frameSize - 1)));
    }

    // High-pass filter coefficient
    float dt = 1.0f / config_.sampleRate;
    float rc = 1.0f / (2.0f * M_PI * config_.highPassFreqHz);
    hpAlpha_ = rc / (rc + dt);
}

OnsetDetector::~OnsetDetector() {
    delete[] windowedFrame_;
    delete[] currentMagnitudes_;
    delete[] previousMagnitudes_;
    delete[] hannWindow_;
}

void OnsetDetector::process(const float* samples, int32_t numSamples) {
    // Apply high-pass filter to isolate attack transients
    // (Work on a copy to avoid modifying input)
    std::vector<float> filtered(numSamples);
    std::memcpy(filtered.data(), samples, numSamples * sizeof(float));
    applyHighPassFilter(filtered.data(), numSamples);

    for (int32_t i = 0; i < numSamples; i++) {
        accumBuffer_[accumPos_++] = filtered[i];
        sampleCounter_++;

        // When we have a full frame, analyze it
        if (accumPos_ >= config_.frameSize) {
            // Apply Hann window
            applyHannWindow(accumBuffer_.data(), windowedFrame_, config_.frameSize);

            // Compute magnitude spectrum
            computeMagnitudeSpectrum(windowedFrame_, currentMagnitudes_);

            // Compute spectral flux
            int32_t specSize = config_.frameSize / 2 + 1;
            float flux = computeSpectralFlux(currentMagnitudes_, previousMagnitudes_, specSize);

            // Store flux in history for adaptive threshold
            fluxHistory_.push_back(flux);
            if ((int32_t)fluxHistory_.size() > config_.medianWindowSize * 2 + 1) {
                fluxHistory_.pop_front();
            }

            // Peak picking with adaptive threshold
            float threshold = computeAdaptiveThreshold();

            if (flux > threshold && flux > prevFlux_ && prevFlux_ > prevPrevFlux_) {
                // We're past a local peak (prevFlux_ was the peak)
                // Nothing to do here - we check on the next iteration
            }

            // Check if the previous frame was a peak
            if (prevFlux_ > threshold &&
                prevFlux_ > prevPrevFlux_ &&
                prevFlux_ > flux) {

                // Get current timestamp
                auto now = std::chrono::steady_clock::now();
                int64_t nowMs = std::chrono::duration_cast<std::chrono::milliseconds>(
                    now.time_since_epoch()
                ).count();

                // Enforce minimum inter-onset interval
                if (nowMs - lastOnsetTimestampMs_ >= config_.minInterOnsetMs) {
                    // Calculate amplitude at onset
                    float maxAmp = 0.0f;
                    for (int32_t j = 0; j < config_.frameSize; j++) {
                        float a = fabsf(accumBuffer_[j]);
                        if (a > maxAmp) maxAmp = a;
                    }

                    detectedOnsets_.push_back({nowMs, maxAmp});
                    lastOnsetTimestampMs_ = nowMs;

                    LOGD("Onset detected: timestamp=%lld, amplitude=%.3f, flux=%.4f",
                         (long long)nowMs, maxAmp, prevFlux_);
                }
            }

            prevPrevFlux_ = prevFlux_;
            prevFlux_ = flux;

            // Copy current magnitudes to previous
            std::memcpy(previousMagnitudes_, currentMagnitudes_, specSize * sizeof(float));

            // Shift buffer by hop size (overlap)
            int32_t remaining = config_.frameSize - config_.hopSize;
            std::memmove(accumBuffer_.data(), accumBuffer_.data() + config_.hopSize,
                        remaining * sizeof(float));
            accumPos_ = remaining;
        }
    }
}

std::vector<OnsetDetector::OnsetEvent> OnsetDetector::getAndClearOnsets() {
    std::vector<OnsetEvent> result;
    result.swap(detectedOnsets_);
    return result;
}

void OnsetDetector::reset() {
    accumPos_ = 0;
    prevFlux_ = 0.0f;
    prevPrevFlux_ = 0.0f;
    lastOnsetTimestampMs_ = 0;
    sampleCounter_ = 0;
    hpFilterState_ = 0.0f;
    fluxHistory_.clear();
    detectedOnsets_.clear();
    std::memset(previousMagnitudes_, 0, (config_.frameSize / 2 + 1) * sizeof(float));
}

void OnsetDetector::applyHannWindow(const float* input, float* output, int32_t size) {
    for (int32_t i = 0; i < size; i++) {
        output[i] = input[i] * hannWindow_[i];
    }
}

void OnsetDetector::computeMagnitudeSpectrum(const float* windowed, float* magnitudes) {
    // Copy to temp buffer for in-place FFT
    std::vector<float> fftData(config_.frameSize);
    std::memcpy(fftData.data(), windowed, config_.frameSize * sizeof(float));

    fft_real(fftData.data(), config_.frameSize);

    // Compute magnitudes from real FFT output
    int32_t halfSize = config_.frameSize / 2;
    magnitudes[0] = fabsf(fftData[0]); // DC component

    for (int32_t i = 1; i < halfSize; i++) {
        // For a real FFT, the output is packed as [R0, R1, I1, R2, I2, ...]
        // Simplified: just use magnitude of the real part for onset detection
        magnitudes[i] = fabsf(fftData[i]);
    }
    magnitudes[halfSize] = fabsf(fftData[halfSize]); // Nyquist
}

float OnsetDetector::computeSpectralFlux(const float* currentMag, const float* prevMag, int32_t size) {
    float flux = 0.0f;
    for (int32_t i = 0; i < size; i++) {
        float diff = currentMag[i] - prevMag[i];
        if (diff > 0.0f) {
            flux += diff; // Half-wave rectification
        }
    }
    return flux;
}

float OnsetDetector::computeAdaptiveThreshold() {
    if (fluxHistory_.empty()) return config_.thresholdOffset;

    // Compute median of flux history
    std::vector<float> sorted(fluxHistory_.begin(), fluxHistory_.end());
    std::sort(sorted.begin(), sorted.end());
    float median = sorted[sorted.size() / 2];

    return median * config_.thresholdMultiplier + config_.thresholdOffset;
}

void OnsetDetector::applyHighPassFilter(float* samples, int32_t numSamples) {
    for (int32_t i = 0; i < numSamples; i++) {
        float input = samples[i];
        float output = hpAlpha_ * (hpFilterState_ + input - (i > 0 ? samples[i - 1] : input));
        hpFilterState_ = output;
        samples[i] = output;
    }
}
