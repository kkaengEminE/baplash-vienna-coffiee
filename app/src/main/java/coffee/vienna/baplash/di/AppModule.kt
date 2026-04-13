package coffee.vienna.baplash.di

import android.content.Context
import androidx.room.Room
import coffee.vienna.baplash.data.local.db.AppDatabase
import coffee.vienna.baplash.data.local.db.dao.ScoreDao
import coffee.vienna.baplash.data.local.db.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rhythmtrainer.db"
        ).build()
    }

    @Provides
    fun provideScoreDao(database: AppDatabase): ScoreDao = database.scoreDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()
}
