package coffee.vienna.baplash.data.repository

import coffee.vienna.baplash.data.local.db.dao.ScoreDao
import coffee.vienna.baplash.data.local.db.entity.BeatEventEntity
import coffee.vienna.baplash.data.local.db.entity.MeasureEntity
import coffee.vienna.baplash.data.local.db.entity.ScoreEntity
import coffee.vienna.baplash.domain.model.BeatEvent
import coffee.vienna.baplash.domain.model.Measure
import coffee.vienna.baplash.domain.model.Score
import coffee.vienna.baplash.domain.model.TimeSignature
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreRepository @Inject constructor(
    private val scoreDao: ScoreDao
) {
    fun getAllScores(): Flow<List<ScoreEntity>> =
        scoreDao.getAllScores()

    suspend fun getScoreById(scoreId: Long): Score? {
        val entity = scoreDao.getScoreById(scoreId) ?: return null
        val measureEntities = scoreDao.getMeasuresForScore(scoreId)

        val measures = measureEntities.map { measure ->
            val beatEvents = scoreDao.getBeatEventsForMeasure(measure.id)
            Measure(
                number = measure.measureNumber,
                beats = beatEvents.map { beat ->
                    BeatEvent(
                        position = beat.beatPosition,
                        duration = beat.duration,
                        isRest = beat.isRest,
                        noteHint = beat.noteNameHint
                    )
                },
                tempoOverride = measure.tempoOverride
            )
        }

        return Score(
            id = entity.id,
            title = entity.title,
            artist = entity.artist,
            bpm = entity.bpm,
            timeSignature = TimeSignature(
                entity.timeSignatureNumerator,
                entity.timeSignatureDenominator
            ),
            measures = measures
        )
    }

    suspend fun saveScore(
        title: String,
        bpm: Int,
        timeSignature: TimeSignature,
        measures: List<Measure>,
        artist: String? = null,
        instrument: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val scoreId = scoreDao.insertScore(
            ScoreEntity(
                title = title,
                artist = artist,
                sourceType = "MANUAL",
                filePath = null,
                bpm = bpm,
                timeSignatureNumerator = timeSignature.numerator,
                timeSignatureDenominator = timeSignature.denominator,
                totalMeasures = measures.size,
                instrument = instrument,
                createdAt = now,
                updatedAt = now
            )
        )

        for (measure in measures) {
            val measureId = scoreDao.insertMeasure(
                MeasureEntity(
                    scoreId = scoreId,
                    measureNumber = measure.number,
                    beatCount = measure.beats.size,
                    tempoOverride = measure.tempoOverride
                )
            )

            scoreDao.insertBeatEvents(
                measure.beats.map { beat ->
                    BeatEventEntity(
                        measureId = measureId,
                        beatPosition = beat.position,
                        duration = beat.duration,
                        isRest = beat.isRest,
                        noteNameHint = beat.noteHint
                    )
                }
            )
        }

        return scoreId
    }

    suspend fun deleteScore(scoreId: Long) {
        scoreDao.getScoreById(scoreId)?.let {
            scoreDao.deleteScore(it)
        }
    }
}
