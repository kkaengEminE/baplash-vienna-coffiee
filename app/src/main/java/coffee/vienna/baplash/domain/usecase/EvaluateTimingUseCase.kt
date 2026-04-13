package coffee.vienna.baplash.domain.usecase

import coffee.vienna.baplash.domain.model.AccuracyRating
import coffee.vienna.baplash.domain.model.BeatAccuracy
import coffee.vienna.baplash.domain.model.Difficulty
import coffee.vienna.baplash.domain.model.OnsetEvent
import javax.inject.Inject
import kotlin.math.abs

class EvaluateTimingUseCase @Inject constructor() {

    /**
     * Evaluate a single beat: compare detected onsets against the expected beat timestamp.
     *
     * @param expectedTimestampMs When the beat should have been played (epoch ms)
     * @param detectedOnsets Recent onset events from the audio engine
     * @param difficulty Current difficulty level (determines tolerance thresholds)
     * @param beatIntervalMs Duration of one beat in ms (60000 / BPM)
     * @param latencyOffsetMs Calibrated audio input latency to compensate
     */
    fun evaluateBeat(
        expectedTimestampMs: Long,
        detectedOnsets: List<OnsetEvent>,
        difficulty: Difficulty,
        beatIntervalMs: Long,
        latencyOffsetMs: Int = 0
    ): BeatAccuracy {
        val toleranceWindow = beatIntervalMs * getToleranceMultiplier(difficulty)

        // Find the onset closest to the expected beat within the tolerance window
        val matchingOnset = detectedOnsets
            .map { onset ->
                val correctedTimestamp = onset.timestampMs - latencyOffsetMs
                val deviation = correctedTimestamp - expectedTimestampMs
                onset to deviation.toFloat()
            }
            .filter { (_, deviation) -> abs(deviation) <= toleranceWindow }
            .minByOrNull { (_, deviation) -> abs(deviation) }

        if (matchingOnset == null) {
            return BeatAccuracy(
                measureNumber = 0,
                beatPosition = 0f,
                deviationMs = null,
                rating = AccuracyRating.MISSED,
                score = 0f
            )
        }

        val (_, deviationMs) = matchingOnset
        val absDeviation = abs(deviationMs)

        val rating = when {
            absDeviation <= difficulty.perfectThresholdMs -> AccuracyRating.PERFECT
            absDeviation <= difficulty.goodThresholdMs -> {
                if (deviationMs < 0) AccuracyRating.EARLY else AccuracyRating.LATE
            }
            deviationMs < 0 -> AccuracyRating.EARLY
            else -> AccuracyRating.LATE
        }

        // Score: 100 at perfect, linearly decreasing to 0 at tolerance boundary
        val score = (1f - (absDeviation / toleranceWindow)).coerceIn(0f, 1f) * 100f

        // Boost for PERFECT and GOOD
        val adjustedScore = when (rating) {
            AccuracyRating.PERFECT -> score.coerceAtLeast(90f)
            AccuracyRating.GOOD -> score.coerceAtLeast(70f)
            else -> score
        }

        return BeatAccuracy(
            measureNumber = 0,
            beatPosition = 0f,
            deviationMs = deviationMs,
            rating = rating,
            score = adjustedScore
        )
    }

    /**
     * Evaluate an entire sequence of expected beats against detected onsets.
     */
    fun evaluateSequence(
        expectedTimestampsMs: List<Long>,
        detectedOnsets: List<OnsetEvent>,
        difficulty: Difficulty,
        beatIntervalMs: Long,
        latencyOffsetMs: Int = 0
    ): List<BeatAccuracy> {
        val usedOnsets = mutableSetOf<Int>()

        return expectedTimestampsMs.mapIndexed { index, expectedMs ->
            val toleranceWindow = beatIntervalMs * getToleranceMultiplier(difficulty)

            val matchingOnset = detectedOnsets
                .mapIndexed { onsetIndex, onset ->
                    val correctedTimestamp = onset.timestampMs - latencyOffsetMs
                    val deviation = correctedTimestamp - expectedMs
                    Triple(onsetIndex, onset, deviation.toFloat())
                }
                .filter { (onsetIndex, _, deviation) ->
                    abs(deviation) <= toleranceWindow && onsetIndex !in usedOnsets
                }
                .minByOrNull { (_, _, deviation) -> abs(deviation) }

            if (matchingOnset == null) {
                BeatAccuracy(
                    measureNumber = index / 4 + 1,
                    beatPosition = (index % 4 + 1).toFloat(),
                    deviationMs = null,
                    rating = AccuracyRating.MISSED,
                    score = 0f
                )
            } else {
                val (onsetIndex, _, deviationMs) = matchingOnset
                usedOnsets.add(onsetIndex)

                val absDeviation = abs(deviationMs)
                val rating = when {
                    absDeviation <= difficulty.perfectThresholdMs -> AccuracyRating.PERFECT
                    absDeviation <= difficulty.goodThresholdMs -> {
                        if (deviationMs < 0) AccuracyRating.EARLY else AccuracyRating.LATE
                    }
                    deviationMs < 0 -> AccuracyRating.EARLY
                    else -> AccuracyRating.LATE
                }

                val score = (1f - (absDeviation / toleranceWindow)).coerceIn(0f, 1f) * 100f
                val adjustedScore = when (rating) {
                    AccuracyRating.PERFECT -> score.coerceAtLeast(90f)
                    AccuracyRating.GOOD -> score.coerceAtLeast(70f)
                    else -> score
                }

                BeatAccuracy(
                    measureNumber = index / 4 + 1,
                    beatPosition = (index % 4 + 1).toFloat(),
                    deviationMs = deviationMs,
                    rating = rating,
                    score = adjustedScore
                )
            }
        }
    }

    private fun getToleranceMultiplier(difficulty: Difficulty): Float = when (difficulty) {
        Difficulty.BEGINNER -> 0.30f
        Difficulty.INTERMEDIATE -> 0.25f
        Difficulty.ADVANCED -> 0.20f
        Difficulty.PRO -> 0.15f
    }
}
