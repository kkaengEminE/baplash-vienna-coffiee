package coffee.vienna.baplash.data.repository

import coffee.vienna.baplash.data.local.db.dao.SessionDao
import coffee.vienna.baplash.data.local.db.entity.PracticeSessionEntity
import coffee.vienna.baplash.data.local.db.entity.TimingEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {
    fun getAllSessions(): Flow<List<PracticeSessionEntity>> =
        sessionDao.getAllSessions()

    fun getRecentSessions(limit: Int = 10): Flow<List<PracticeSessionEntity>> =
        sessionDao.getRecentSessions(limit)

    fun getSessionsForScore(scoreId: Long): Flow<List<PracticeSessionEntity>> =
        sessionDao.getSessionsForScore(scoreId)

    suspend fun getSessionById(sessionId: Long): PracticeSessionEntity? =
        sessionDao.getSessionById(sessionId)

    suspend fun saveSession(session: PracticeSessionEntity): Long =
        sessionDao.insertSession(session)

    suspend fun updateSession(session: PracticeSessionEntity) =
        sessionDao.updateSession(session)

    suspend fun saveTimingEvents(events: List<TimingEventEntity>) =
        sessionDao.insertTimingEvents(events)

    suspend fun getTimingEventsForSession(sessionId: Long): List<TimingEventEntity> =
        sessionDao.getTimingEventsForSession(sessionId)

    fun getTotalSessionCount(): Flow<Int> =
        sessionDao.getTotalSessionCount()

    fun getTotalPracticeTimeMs(): Flow<Long?> =
        sessionDao.getTotalPracticeTimeMs()
}
