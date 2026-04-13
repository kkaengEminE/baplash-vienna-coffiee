package coffee.vienna.baplash.domain.model

data class Score(
    val id: Long = 0,
    val title: String,
    val artist: String? = null,
    val bpm: Int,
    val timeSignature: TimeSignature,
    val measures: List<Measure>,
    val instrument: Instrument? = null
)

data class TimeSignature(
    val numerator: Int,
    val denominator: Int
) {
    override fun toString(): String = "$numerator/$denominator"
}

data class Measure(
    val number: Int,
    val beats: List<BeatEvent>,
    val tempoOverride: Int? = null
)

data class BeatEvent(
    val position: Float,
    val duration: Float,
    val isRest: Boolean = false,
    val noteHint: String? = null
)

enum class Instrument(val displayName: String) {
    BASS("베이스"),
    GUITAR("기타"),
    DRUMS("드럼"),
    KEYS("키보드"),
    VOICE("보컬"),
    OTHER("기타 악기")
}

data class PracticeConfig(
    val score: Score? = null,
    val bpm: Int = 120,
    val timeSignature: TimeSignature = TimeSignature(4, 4),
    val startMeasure: Int = 1,
    val endMeasure: Int = 1,
    val loopEnabled: Boolean = false,
    val gradualTempo: GradualTempo? = null,
    val difficulty: Difficulty = Difficulty.INTERMEDIATE,
    val countInBeats: Int = 4
)

data class GradualTempo(
    val startBpm: Int,
    val targetBpm: Int,
    val incrementBpm: Int = 5,
    val afterSuccessfulLoops: Int = 2
)

enum class Difficulty(val displayName: String, val perfectThresholdMs: Float, val goodThresholdMs: Float) {
    BEGINNER("초급", 30f, 60f),
    INTERMEDIATE("중급", 20f, 40f),
    ADVANCED("상급", 10f, 25f),
    PRO("프로", 5f, 15f)
}

data class BeatAccuracy(
    val measureNumber: Int,
    val beatPosition: Float,
    val deviationMs: Float?,
    val rating: AccuracyRating,
    val score: Float
)

enum class AccuracyRating {
    PERFECT,
    GOOD,
    EARLY,
    LATE,
    MISSED,
    EXTRA
}

data class SessionSummary(
    val overallScore: Float,
    val totalBeats: Int,
    val perfectCount: Int,
    val goodCount: Int,
    val earlyCount: Int,
    val lateCount: Int,
    val missedCount: Int,
    val extraNoteCount: Int,
    val averageDeviationMs: Float,
    val consistencyScore: Float,
    val perMeasureScores: List<MeasureScore>,
    val bpmUsed: Int,
    val durationMs: Long
)

data class MeasureScore(
    val measureNumber: Int,
    val score: Float,
    val beatAccuracies: List<BeatAccuracy>
)

data class OnsetEvent(
    val timestampMs: Long,
    val amplitude: Float
)
