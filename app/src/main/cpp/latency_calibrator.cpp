#include "latency_calibrator.h"

#include <algorithm>
#include <cmath>
#include <numeric>

void LatencyCalibrator::addExpectedBeatMs(int64_t timestampMs) {
    expectedBeatTimestamps_.push_back(timestampMs);
}

void LatencyCalibrator::addTapMs(int64_t timestampMs) {
    tapTimestamps_.push_back(timestampMs);
}

int32_t LatencyCalibrator::calculateLatencyMs() const {
    if (tapTimestamps_.size() < 4 || expectedBeatTimestamps_.empty()) {
        return -1;
    }

    // Match each tap to its nearest expected beat
    std::vector<int64_t> deviations;

    for (int64_t tapMs : tapTimestamps_) {
        int64_t bestDiff = INT64_MAX;

        for (int64_t beatMs : expectedBeatTimestamps_) {
            int64_t diff = tapMs - beatMs;
            if (std::abs(diff) < std::abs(bestDiff)) {
                bestDiff = diff;
            }
        }

        // Only include reasonable deviations (within 500ms)
        if (std::abs(bestDiff) < 500) {
            deviations.push_back(bestDiff);
        }
    }

    if (deviations.size() < 4) {
        return -1;
    }

    // Remove outliers (outside 2 standard deviations)
    double mean = std::accumulate(deviations.begin(), deviations.end(), 0.0) / deviations.size();
    double variance = 0.0;
    for (int64_t d : deviations) {
        double diff = d - mean;
        variance += diff * diff;
    }
    variance /= deviations.size();
    double stddev = std::sqrt(variance);

    std::vector<int64_t> filtered;
    for (int64_t d : deviations) {
        if (std::abs(d - mean) <= 2.0 * stddev) {
            filtered.push_back(d);
        }
    }

    if (filtered.empty()) {
        return static_cast<int32_t>(std::round(mean));
    }

    // Return mean of filtered deviations
    double filteredMean = std::accumulate(filtered.begin(), filtered.end(), 0.0) / filtered.size();
    return static_cast<int32_t>(std::round(filteredMean));
}

void LatencyCalibrator::reset() {
    expectedBeatTimestamps_.clear();
    tapTimestamps_.clear();
}
