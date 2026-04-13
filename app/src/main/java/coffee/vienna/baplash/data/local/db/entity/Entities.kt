package coffee.vienna.baplash.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String?,
    val sourceType: String,
    val filePath: String?,
    val bpm: Int,
    val timeSignatureNumerator: Int,
    val timeSignatureDenominator: Int,
    val totalMeasures: Int,
    val instrument: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "measures",
    foreignKeys = [ForeignKey(
        entity = ScoreEntity::class,
        parentColumns = ["id"],
        childColumns = ["scoreId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("scoreId")]
)
data class MeasureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scoreId: Long,
    val measureNumber: Int,
    val beatCount: Int,
    val tempoOverride: Int?
)

@Entity(
    tableName = "beat_events",
    foreignKeys = [ForeignKey(
        entity = MeasureEntity::class,
        parentColumns = ["id"],
        childColumns = ["measureId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("measureId")]
)
data class BeatEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measureId: Long,
    val beatPosition: Float,
    val duration: Float,
    val isRest: Boolean,
    val noteNameHint: String?
)

@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scoreId: Long?,
    val startedAt: Long,
    val endedAt: Long?,
    val bpmUsed: Int,
    val startMeasure: Int?,
    val endMeasure: Int?,
    val difficultyLevel: String,
    val overallScore: Float?,
    val totalBeatsExpected: Int,
    val totalBeatsHit: Int,
    val totalBeatsMissed: Int,
    val averageDeviationMs: Float?
)

@Entity(
    tableName = "timing_events",
    foreignKeys = [ForeignKey(
        entity = PracticeSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class TimingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val measureNumber: Int,
    val beatPosition: Float,
    val expectedTimestampMs: Long,
    val detectedTimestampMs: Long?,
    val deviationMs: Float?,
    val accuracy: String,
    val score: Float
)
