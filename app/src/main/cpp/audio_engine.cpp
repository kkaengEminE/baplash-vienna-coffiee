#include "audio_engine.h"

#include <android/log.h>
#include <chrono>

#define LOG_TAG "AudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AudioEngine::AudioEngine()
    : analysisRunning_(false),
      capturing_(false),
      latencyOffsetMs_(0),
      sampleRate_(44100) {

    ringBuffer_ = std::make_unique<RingBuffer>(RING_BUFFER_SIZE);

    OnsetDetector::Config detectorConfig;
    detectorConfig.sampleRate = sampleRate_;
    onsetDetector_ = std::make_unique<OnsetDetector>(detectorConfig);

    metronome_ = std::make_unique<Metronome>(sampleRate_);

    LOGI("AudioEngine created, sampleRate=%d", sampleRate_);
}

AudioEngine::~AudioEngine() {
    destroy();
}

void AudioEngine::startCapture() {
    if (capturing_.load()) return;

    // Create input stream for microphone capture
    oboe::AudioStreamBuilder inputBuilder;
    inputBuilder.setDirection(oboe::Direction::Input)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setSampleRate(sampleRate_)
        ->setChannelCount(oboe::ChannelCount::Mono)
        ->setFormat(oboe::AudioFormat::Float)
        ->setDataCallback(this);

    oboe::Result result = inputBuilder.openStream(inputStream_);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open input stream: %s", oboe::convertToText(result));
        return;
    }

    sampleRate_ = inputStream_->getSampleRate();
    LOGI("Input stream opened: sampleRate=%d, framesPerBurst=%d",
         sampleRate_, inputStream_->getFramesPerBurst());

    result = inputStream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start input stream: %s", oboe::convertToText(result));
        return;
    }

    capturing_.store(true);
    startAnalysisThread();

    LOGI("Audio capture started");
}

void AudioEngine::stopCapture() {
    if (!capturing_.load()) return;

    stopAnalysisThread();

    if (inputStream_) {
        inputStream_->requestStop();
        inputStream_->close();
        inputStream_.reset();
    }

    capturing_.store(false);
    ringBuffer_->reset();
    onsetDetector_->reset();

    LOGI("Audio capture stopped");
}

void AudioEngine::startMetronome(int32_t bpm, int32_t beatsPerMeasure) {
    metronome_->setBpm(bpm);
    metronome_->setBeatsPerMeasure(beatsPerMeasure);

    if (!outputStream_) {
        // Create output stream for metronome
        oboe::AudioStreamBuilder outputBuilder;
        outputBuilder.setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setSampleRate(sampleRate_)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setFormat(oboe::AudioFormat::Float);

        oboe::Result result = outputBuilder.openStream(outputStream_);
        if (result != oboe::Result::OK) {
            LOGE("Failed to open output stream: %s", oboe::convertToText(result));
            return;
        }
    }

    metronome_->start();

    // Start output stream (render metronome clicks in callback)
    // Note: We use a simple blocking write approach here
    // In production, use a separate callback for output
    outputStream_->requestStart();

    LOGI("Metronome started: BPM=%d, beatsPerMeasure=%d", bpm, beatsPerMeasure);
}

void AudioEngine::stopMetronome() {
    metronome_->stop();

    if (outputStream_) {
        outputStream_->requestStop();
        outputStream_->close();
        outputStream_.reset();
    }

    LOGI("Metronome stopped");
}

void AudioEngine::setBpm(int32_t bpm) {
    metronome_->setBpm(bpm);
}

std::vector<std::pair<float, float>> AudioEngine::getOnsets() {
    std::lock_guard<std::mutex> lock(onsetsMutex_);
    std::vector<std::pair<float, float>> result;
    result.swap(collectedOnsets_);
    return result;
}

void AudioEngine::destroy() {
    stopCapture();
    stopMetronome();
    LOGI("AudioEngine destroyed");
}

oboe::DataCallbackResult AudioEngine::onAudioReady(
    oboe::AudioStream* stream,
    void* audioData,
    int32_t numFrames
) {
    auto* floatData = static_cast<float*>(audioData);

    if (stream->getDirection() == oboe::Direction::Input) {
        // Write captured audio to ring buffer for analysis
        ringBuffer_->write(floatData, numFrames);
    } else {
        // Render metronome output
        metronome_->renderAudio(floatData, numFrames);
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::startAnalysisThread() {
    analysisRunning_.store(true);
    analysisThread_ = std::thread(&AudioEngine::analysisThreadFunc, this);
}

void AudioEngine::stopAnalysisThread() {
    analysisRunning_.store(false);
    if (analysisThread_.joinable()) {
        analysisThread_.join();
    }
}

void AudioEngine::analysisThreadFunc() {
    LOGI("Analysis thread started");

    float analysisBuffer[ANALYSIS_BUFFER_SIZE];

    while (analysisRunning_.load()) {
        int32_t available = ringBuffer_->availableToRead();

        if (available >= ANALYSIS_BUFFER_SIZE) {
            int32_t read = ringBuffer_->read(analysisBuffer, ANALYSIS_BUFFER_SIZE);
            if (read > 0) {
                onsetDetector_->process(analysisBuffer, read);

                auto onsets = onsetDetector_->getAndClearOnsets();
                if (!onsets.empty()) {
                    std::lock_guard<std::mutex> lock(onsetsMutex_);
                    for (const auto& onset : onsets) {
                        collectedOnsets_.emplace_back(
                            static_cast<float>(onset.timestampMs),
                            onset.amplitude
                        );
                    }
                }
            }
        } else {
            // Sleep briefly to avoid busy-waiting
            std::this_thread::sleep_for(std::chrono::milliseconds(2));
        }
    }

    LOGI("Analysis thread stopped");
}
