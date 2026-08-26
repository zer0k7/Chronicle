package io.chronicle.usagestats.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.chronicle.usagestats.core.util.Constants
import io.chronicle.usagestats.data.local.ChronicleDatabase
import io.chronicle.usagestats.data.local.dao.AppOverrideDao
import io.chronicle.usagestats.data.local.dao.UsageDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ChronicleDatabase {
        return Room.databaseBuilder(
            context,
            ChronicleDatabase::class.java,
            Constants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideUsageDao(database: ChronicleDatabase): UsageDao {
        return database.usageDao()
    }

    @Provides
    @Singleton
    fun provideAppOverrideDao(database: ChronicleDatabase): AppOverrideDao {
        return database.appOverrideDao()
    }

    @Provides
    @Singleton
    fun provideNetworkUsageDao(database: ChronicleDatabase): io.chronicle.usagestats.data.local.dao.NetworkUsageDao {
        return database.networkUsageDao()
    }
}
