#pragma once

#include <cstdint>
#include <vector>

/**
 * Tap-based latency calibration.
 *
 * The user taps the screen in sync with metronome beats.
 * We measure the difference between the expected beat timestamps
 * and the actual tap timestamps to estimate system latency.
 */
class LatencyCalibrator {
public:
    LatencyCalibrator() = default;

    void addExpectedBeatMs(int64_t timestampMs);
    void addTapMs(int64_t timestampMs);

    /**
     * Calculate the estimated latency offset.
     * Requires at least 4 tap/beat pairs.
     * @return Estimated latency in ms, or -1 if insufficient data.
     */
    int32_t calculateLatencyMs() const;

    void reset();
    int32_t getSampleCount() const { return static_cast<int32_t>(tapTimestamps_.size()); }

private:
    std::vector<int64_t> expectedBeatTimestamps_;
    std::vector<int64_t> tapTimestamps_;
};
