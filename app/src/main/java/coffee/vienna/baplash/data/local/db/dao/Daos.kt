package coffee.vienna.baplash.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import coffee.vienna.baplash.data.local.db.entity.BeatEventEntity
import coffee.vienna.baplash.data.local.db.entity.MeasureEntity
import coffee.vienna.baplash.data.local.db.entity.PracticeSessionEntity
import coffee.vienna.baplash.data.local.db.entity.ScoreEntity
import coffee.vienna.baplash.data.local.db.entity.TimingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores ORDER BY updatedAt DESC")
    fun getAllScores(): Flow<List<ScoreEntity>>

    @Query("SELECT * FROM scores WHERE id = :scoreId")
    suspend fun getScoreById(scoreId: Long): ScoreEntity?

    @Insert
    suspend fun insertScore(score: ScoreEntity): Long

    @Update
    suspend fun updateScore(score: ScoreEntity)

    @Delete
    suspend fun deleteScore(score: ScoreEntity)

    @Query("SELECT * FROM measures WHERE scoreId = :scoreId ORDER BY measureNumber")
    suspend fun getMeasuresForScore(scoreId: Long): List<MeasureEntity>

    @Insert
    suspend fun insertMeasure(measure: MeasureEntity): Long

    @Insert
    suspend fun insertMeasures(measures: List<MeasureEntity>): List<Long>

    @Query("SELECT * FROM beat_events WHERE measureId = :measureId ORDER BY beatPosition")
    suspend fun getBeatEventsForMeasure(measureId: Long): List<BeatEventEntity>

    @Insert
    suspend fun insertBeatEvents(events: List<BeatEventEntity>)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM practice_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions WHERE scoreId = :scoreId ORDER BY startedAt DESC")
    fun getSessionsForScore(scoreId: Long): Flow<List<PracticeSessionEntity>>

    @Insert
    suspend fun insertSession(session: PracticeSessionEntity): Long

    @Update
    suspend fun updateSession(session: PracticeSessionEntity)

    @Query("SELECT * FROM timing_events WHERE sessionId = :sessionId ORDER BY expectedTimestampMs")
    suspend fun getTimingEventsForSession(sessionId: Long): List<TimingEventEntity>

    @Insert
    suspend fun insertTimingEvents(events: List<TimingEventEntity>)

    @Query("SELECT AVG(overallScore) FROM practice_sessions WHERE scoreId = :scoreId")
    suspend fun getAverageScoreForScore(scoreId: Long): Float?

    @Query("SELECT COUNT(*) FROM practice_sessions")
    fun getTotalSessionCount(): Flow<Int>

    @Query("SELECT SUM(endedAt - startedAt) FROM practice_sessions WHERE endedAt IS NOT NULL")
    fun getTotalPracticeTimeMs(): Flow<Long?>
}
