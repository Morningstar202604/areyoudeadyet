package com.silema.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.silema.app.db.SilemaDatabase
import com.silema.app.db.dao.ContactDao
import com.silema.app.db.dao.VitalRecordDao
import com.silema.app.db.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 数据库模块。
 *
 * 提供 [SilemaDatabase] 及其 DAO 的单例。
 * 开发期使用 fallbackToDestructiveMigration；正式发布应替换为显式 Migration。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SilemaDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            SilemaDatabase::class.java,
            SilemaDatabase.DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideVitalRecordDao(db: SilemaDatabase): VitalRecordDao = db.vitalRecordDao()

    @Provides
    fun provideContactDao(db: SilemaDatabase): ContactDao = db.contactDao()

    @Provides
    fun provideWorkoutDao(db: SilemaDatabase): WorkoutDao = db.workoutDao()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("silema_prefs", Context.MODE_PRIVATE)
}
