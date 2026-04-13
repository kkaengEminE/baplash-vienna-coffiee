#pragma once

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <cstring>

/**
 * Lock-free single-producer single-consumer ring buffer for audio samples.
 * Used to pass audio data from the Oboe callback thread to the analysis thread.
 */
class RingBuffer {
public:
    explicit RingBuffer(int32_t capacity)
        : capacity_(capacity),
          buffer_(new float[capacity]),
          readPos_(0),
          writePos_(0) {
        std::memset(buffer_, 0, capacity * sizeof(float));
    }

    ~RingBuffer() {
        delete[] buffer_;
    }

    // Non-copyable
    RingBuffer(const RingBuffer&) = delete;
    RingBuffer& operator=(const RingBuffer&) = delete;

    /**
     * Write samples to the buffer. Called from the audio callback thread (producer).
     * @return Number of samples actually written.
     */
    int32_t write(const float* data, int32_t numSamples) {
        int32_t available = capacity_ - availableToRead();
        int32_t toWrite = std::min(numSamples, available);

        int32_t wp = writePos_.load(std::memory_order_relaxed);
        for (int32_t i = 0; i < toWrite; i++) {
            buffer_[(wp + i) % capacity_] = data[i];
        }
        writePos_.store((wp + toWrite) % capacity_, std::memory_order_release);

        return toWrite;
    }

    /**
     * Read samples from the buffer. Called from the analysis thread (consumer).
     * @return Number of samples actually read.
     */
    int32_t read(float* dest, int32_t numSamples) {
        int32_t available = availableToRead();
        int32_t toRead = std::min(numSamples, available);

        int32_t rp = readPos_.load(std::memory_order_relaxed);
        for (int32_t i = 0; i < toRead; i++) {
            dest[i] = buffer_[(rp + i) % capacity_];
        }
        readPos_.store((rp + toRead) % capacity_, std::memory_order_release);

        return toRead;
    }

    int32_t availableToRead() const {
        int32_t wp = writePos_.load(std::memory_order_acquire);
        int32_t rp = readPos_.load(std::memory_order_relaxed);
        return (wp - rp + capacity_) % capacity_;
    }

    void reset() {
        readPos_.store(0, std::memory_order_relaxed);
        writePos_.store(0, std::memory_order_relaxed);
    }

private:
    int32_t capacity_;
    float* buffer_;
    std::atomic<int32_t> readPos_;
    std::atomic<int32_t> writePos_;
};
