package coffee.vienna.baplash.audio

import coffee.vienna.baplash.domain.model.OnsetEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kotlin-side binding to the native C++ audio engine via JNI.
 * Manages Oboe audio streams for microphone capture and metronome output.
 */
@Singleton
class AudioEngineBinding @Inject constructor() {

    private var isEngineInitialized = false
    private var isCapturing = false
    private var isMetronomePlaying = false

    init {
        System.loadLibrary("rhythmtrainer_audio")
        isEngineInitialized = true
    }

    fun startAudioCapture() {
        if (!isCapturing) {
            nativeStartCapture()
            isCapturing = true
        }
    }

    fun stopAudioCapture() {
        if (isCapturing) {
            nativeStopCapture()
            isCapturing = false
        }
    }

    fun startMetronome(bpm: Int, beatsPerMeasure: Int) {
        nativeStartMetronome(bpm, beatsPerMeasure)
        isMetronomePlaying = true
    }

    fun stopMetronome() {
        if (isMetronomePlaying) {
            nativeStopMetronome()
            isMetronomePlaying = false
        }
    }

    fun setMetronomeBpm(bpm: Int) {
        nativeSetBpm(bpm)
    }

    /**
     * Returns onset events detected since the last call.
     * Each onset has a timestamp (ms since epoch) and amplitude.
     */
    fun getRecentOnsets(): List<OnsetEvent> {
        val raw = nativeGetOnsets()
        // raw format: [timestamp1, amplitude1, timestamp2, amplitude2, ...]
        val onsets = mutableListOf<OnsetEvent>()
        var i = 0
        while (i < raw.size - 1) {
            onsets.add(OnsetEvent(
                timestampMs = raw[i].toLong(),
                amplitude = raw[i + 1]
            ))
            i += 2
        }
        return onsets
    }

    /**
     * Returns the calibrated audio input latency in milliseconds.
     */
    fun getLatencyOffsetMs(): Int = nativeGetLatencyMs()

    fun setLatencyOffsetMs(offsetMs: Int) {
        nativeSetLatencyMs(offsetMs)
    }

    fun destroy() {
        stopAudioCapture()
        stopMetronome()
        nativeDestroy()
        isEngineInitialized = false
    }

    // --- Native JNI methods ---

    private external fun nativeStartCapture()
    private external fun nativeStopCapture()
    private external fun nativeStartMetronome(bpm: Int, beatsPerMeasure: Int)
    private external fun nativeStopMetronome()
    private external fun nativeSetBpm(bpm: Int)
    private external fun nativeGetOnsets(): FloatArray
    private external fun nativeGetLatencyMs(): Int
    private external fun nativeSetLatencyMs(ms: Int)
    private external fun nativeDestroy()
}
