package coffee.vienna.baplash.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coffee.vienna.baplash.audio.AudioEngineBinding
import coffee.vienna.baplash.domain.model.AccuracyRating
import coffee.vienna.baplash.domain.model.BeatAccuracy
import coffee.vienna.baplash.domain.model.Difficulty
import coffee.vienna.baplash.domain.usecase.EvaluateTimingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PracticeUiState(
    val bpm: Int = 120,
    val timeSignature: String = "4/4",
    val totalBeatsInMeasure: Int = 4,
    val currentBeat: Int = 0,
    val currentMeasure: Int = 1,
    val currentScore: Float = 0f,
    val isPlaying: Boolean = false,
    val isCountingIn: Boolean = false,
    val lastAccuracy: AccuracyRating? = null,
    val lastDeviationMs: Float? = null,
    val recentBeatAccuracies: List<AccuracyRating?> = List(4) { null },
    val beatAccuracies: List<BeatAccuracy> = emptyList()
)

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val audioEngine: AudioEngineBinding,
    private val evaluateTimingUseCase: EvaluateTimingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private var metronomeJob: Job? = null
    private var practiceStartTimeMs: Long = 0
    private var allBeatAccuracies = mutableListOf<BeatAccuracy>()
    private var totalBeatsPlayed = 0

    fun setBpm(bpm: Int) {
        _uiState.update { it.copy(bpm = bpm.coerceIn(40, 240)) }
    }

    fun startPractice() {
        val state = _uiState.value
        practiceStartTimeMs = System.currentTimeMillis()
        allBeatAccuracies.clear()
        totalBeatsPlayed = 0

        _uiState.update {
            it.copy(
                isPlaying = true,
                isCountingIn = true,
                currentBeat = 0,
                currentMeasure = 1,
                currentScore = 0f,
                lastAccuracy = null,
                lastDeviationMs = null,
                recentBeatAccuracies = List(state.totalBeatsInMeasure) { null }
            )
        }

        audioEngine.startMetronome(state.bpm, state.totalBeatsInMeasure)
        audioEngine.startAudioCapture()

        metronomeJob = viewModelScope.launch {
            val beatIntervalMs = 60_000L / state.bpm

            // Count-in: 4 beats
            for (i in 0 until 4) {
                _uiState.update { it.copy(currentBeat = i) }
                delay(beatIntervalMs)
            }

            _uiState.update {
                it.copy(
                    isCountingIn = false,
                    currentBeat = 0,
                    recentBeatAccuracies = List(state.totalBeatsInMeasure) { null }
                )
            }

            // Main practice loop
            var beatIndex = 0
            var measureIndex = 1
            while (true) {
                val beatInMeasure = beatIndex % state.totalBeatsInMeasure

                _uiState.update {
                    it.copy(
                        currentBeat = beatInMeasure,
                        currentMeasure = measureIndex
                    )
                }

                val expectedTimestampMs = practiceStartTimeMs +
                    (4 * beatIntervalMs) + // count-in offset
                    (beatIndex * beatIntervalMs)

                // Check for onset near this beat
                val detectedOnsets = audioEngine.getRecentOnsets()
                val accuracy = evaluateTimingUseCase.evaluateBeat(
                    expectedTimestampMs = expectedTimestampMs,
                    detectedOnsets = detectedOnsets,
                    difficulty = Difficulty.INTERMEDIATE,
                    beatIntervalMs = beatIntervalMs
                )

                allBeatAccuracies.add(accuracy)
                totalBeatsPlayed++

                val updatedMeasureAccuracies = _uiState.value.recentBeatAccuracies.toMutableList()
                updatedMeasureAccuracies[beatInMeasure] = accuracy.rating

                val overallScore = if (allBeatAccuracies.isNotEmpty()) {
                    allBeatAccuracies.map { it.score }.average().toFloat()
                } else 0f

                _uiState.update {
                    it.copy(
                        lastAccuracy = accuracy.rating,
                        lastDeviationMs = accuracy.deviationMs,
                        recentBeatAccuracies = updatedMeasureAccuracies,
                        currentScore = overallScore
                    )
                }

                // Reset measure accuracies at measure boundary
                if (beatInMeasure == state.totalBeatsInMeasure - 1) {
                    measureIndex++
                    delay(beatIntervalMs)
                    _uiState.update {
                        it.copy(
                            recentBeatAccuracies = List(state.totalBeatsInMeasure) { null }
                        )
                    }
                } else {
                    delay(beatIntervalMs)
                }

                beatIndex++
            }
        }
    }

    fun stopPractice() {
        metronomeJob?.cancel()
        metronomeJob = null
        audioEngine.stopMetronome()
        audioEngine.stopAudioCapture()

        _uiState.update {
            it.copy(
                isPlaying = false,
                isCountingIn = false,
                beatAccuracies = allBeatAccuracies.toList()
            )
        }
    }

    fun resetSession() {
        stopPractice()
        allBeatAccuracies.clear()
        totalBeatsPlayed = 0
        _uiState.update {
            PracticeUiState(bpm = it.bpm)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPractice()
    }
}
