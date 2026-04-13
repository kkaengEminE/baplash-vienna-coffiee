package coffee.vienna.baplash.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import coffee.vienna.baplash.data.local.db.dao.ScoreDao
import coffee.vienna.baplash.data.local.db.dao.SessionDao
import coffee.vienna.baplash.data.local.db.entity.BeatEventEntity
import coffee.vienna.baplash.data.local.db.entity.MeasureEntity
import coffee.vienna.baplash.data.local.db.entity.PracticeSessionEntity
import coffee.vienna.baplash.data.local.db.entity.ScoreEntity
import coffee.vienna.baplash.data.local.db.entity.TimingEventEntity

@Database(
    entities = [
        ScoreEntity::class,
        MeasureEntity::class,
        BeatEventEntity::class,
        PracticeSessionEntity::class,
        TimingEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
    abstract fun sessionDao(): SessionDao
}
