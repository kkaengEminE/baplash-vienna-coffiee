package coffee.vienna.baplash.domain.usecase

import coffee.vienna.baplash.domain.model.AccuracyRating
import coffee.vienna.baplash.domain.model.Difficulty
import coffee.vienna.baplash.domain.model.OnsetEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class EvaluateTimingUseCaseTest {

    private lateinit var useCase: EvaluateTimingUseCase

    // 120 BPM = 500ms per beat
    private val beatIntervalMs = 500L

    @Before
    fun setup() {
        useCase = EvaluateTimingUseCase()
    }

    @Test
    fun `perfect timing returns PERFECT rating`() {
        val expectedMs = 1000L
        val onsets = listOf(OnsetEvent(timestampMs = 1000L, amplitude = 0.8f))

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.PERFECT)
        assertThat(result.score).isAtLeast(90f)
        assertThat(result.deviationMs).isEqualTo(0f)
    }

    @Test
    fun `slight early timing within perfect threshold returns PERFECT`() {
        val expectedMs = 1000L
        val onsets = listOf(OnsetEvent(timestampMs = 985L, amplitude = 0.8f))

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.PERFECT)
        assertThat(result.deviationMs).isEqualTo(-15f)
    }

    @Test
    fun `early timing beyond perfect threshold returns EARLY`() {
        val expectedMs = 1000L
        val onsets = listOf(OnsetEvent(timestampMs = 950L, amplitude = 0.8f))

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.EARLY)
        assertThat(result.deviationMs).isLessThan(0f)
    }

    @Test
    fun `late timing returns LATE`() {
        val expectedMs = 1000L
        val onsets = listOf(OnsetEvent(timestampMs = 1060L, amplitude = 0.8f))

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.LATE)
        assertThat(result.deviationMs).isGreaterThan(0f)
    }

    @Test
    fun `no onset detected returns MISSED`() {
        val expectedMs = 1000L
        val onsets = emptyList<OnsetEvent>()

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.MISSED)
        assertThat(result.score).isEqualTo(0f)
        assertThat(result.deviationMs).isNull()
    }

    @Test
    fun `onset outside tolerance window returns MISSED`() {
        val expectedMs = 1000L
        // Tolerance = 500 * 0.25 = 125ms, so 200ms away is outside
        val onsets = listOf(OnsetEvent(timestampMs = 1200L, amplitude = 0.8f))

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.MISSED)
    }

    @Test
    fun `closest onset is selected when multiple onsets present`() {
        val expectedMs = 1000L
        val onsets = listOf(
            OnsetEvent(timestampMs = 930L, amplitude = 0.5f),  // -70ms
            OnsetEvent(timestampMs = 1005L, amplitude = 0.8f), // +5ms (closest)
            OnsetEvent(timestampMs = 1080L, amplitude = 0.6f)  // +80ms
        )

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.PERFECT)
        assertThat(result.deviationMs).isEqualTo(5f)
    }

    @Test
    fun `latency offset is applied correctly`() {
        val expectedMs = 1000L
        // Onset at 1020ms, but with 20ms latency offset, corrected to 1000ms = PERFECT
        val onsets = listOf(OnsetEvent(timestampMs = 1020L, amplitude = 0.8f))

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs,
            latencyOffsetMs = 20
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.PERFECT)
        assertThat(result.deviationMs).isEqualTo(0f)
    }

    @Test
    fun `beginner difficulty has wider tolerance`() {
        val expectedMs = 1000L
        // 25ms early - would be EARLY in intermediate but PERFECT in beginner
        val onsets = listOf(OnsetEvent(timestampMs = 975L, amplitude = 0.8f))

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.BEGINNER,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.PERFECT)
    }

    @Test
    fun `pro difficulty has tighter tolerance`() {
        val expectedMs = 1000L
        // 10ms early - PERFECT in intermediate but EARLY in pro (threshold is 5ms)
        val onsets = listOf(OnsetEvent(timestampMs = 990L, amplitude = 0.8f))

        val result = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = onsets,
            difficulty = Difficulty.PRO,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(result.rating).isEqualTo(AccuracyRating.EARLY)
    }

    @Test
    fun `evaluate sequence matches onsets to beats without double-counting`() {
        val expectedTimestamps = listOf(1000L, 1500L, 2000L, 2500L)
        val onsets = listOf(
            OnsetEvent(timestampMs = 1005L, amplitude = 0.8f),  // Beat 1: +5ms
            OnsetEvent(timestampMs = 1510L, amplitude = 0.7f),  // Beat 2: +10ms
            // Beat 3: missing
            OnsetEvent(timestampMs = 2490L, amplitude = 0.6f)   // Beat 4: -10ms
        )

        val results = useCase.evaluateSequence(
            expectedTimestampsMs = expectedTimestamps,
            detectedOnsets = onsets,
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(results).hasSize(4)
        assertThat(results[0].rating).isEqualTo(AccuracyRating.PERFECT)
        assertThat(results[1].rating).isEqualTo(AccuracyRating.PERFECT)
        assertThat(results[2].rating).isEqualTo(AccuracyRating.MISSED)
        assertThat(results[3].rating).isEqualTo(AccuracyRating.PERFECT)
    }

    @Test
    fun `score decreases linearly with deviation`() {
        val expectedMs = 1000L

        val perfectResult = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = listOf(OnsetEvent(1000L, 0.8f)),
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        val earlyResult = useCase.evaluateBeat(
            expectedTimestampMs = expectedMs,
            detectedOnsets = listOf(OnsetEvent(950L, 0.8f)),
            difficulty = Difficulty.INTERMEDIATE,
            beatIntervalMs = beatIntervalMs
        )

        assertThat(perfectResult.score).isGreaterThan(earlyResult.score)
    }
}
