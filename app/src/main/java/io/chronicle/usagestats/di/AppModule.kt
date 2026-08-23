package io.chronicle.usagestats.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.chronicle.usagestats.core.util.AppIconHelper
import io.chronicle.usagestats.data.local.dao.UsageDao
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.data.repository.UsageStatsRepositoryImpl
import io.chronicle.usagestats.domain.repository.UsageRepository
import io.chronicle.usagestats.domain.usecase.ExportPdfReportUseCase
import io.chronicle.usagestats.domain.usecase.ExportReportImageUseCase
import io.chronicle.usagestats.domain.usecase.GetReportUseCase
import io.chronicle.usagestats.domain.usecase.GetTimelineUsageUseCase
import io.chronicle.usagestats.domain.usecase.SyncUsageDataUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppIconHelper(
        @ApplicationContext context: Context
    ): AppIconHelper {
        return AppIconHelper(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        @ApplicationContext context: Context
    ): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideUsageRepository(
        @ApplicationContext context: Context,
        usageDao: UsageDao,
        appIconHelper: AppIconHelper
    ): UsageRepository {
        return UsageStatsRepositoryImpl(context, usageDao, appIconHelper)
    }

    @Provides
    @Singleton
    fun provideGetTimelineUsageUseCase(
        usageRepository: UsageRepository
    ): GetTimelineUsageUseCase {
        return GetTimelineUsageUseCase(usageRepository)
    }

    @Provides
    @Singleton
    fun provideGetReportUseCase(
        usageRepository: UsageRepository
    ): GetReportUseCase {
        return GetReportUseCase(usageRepository)
    }

    @Provides
    @Singleton
    fun provideSyncUsageDataUseCase(
        usageRepository: UsageRepository
    ): SyncUsageDataUseCase {
        return SyncUsageDataUseCase(usageRepository)
    }

    @Provides
    @Singleton
    fun provideExportPdfReportUseCase(
        @ApplicationContext context: Context
    ): ExportPdfReportUseCase {
        return ExportPdfReportUseCase(context)
    }

    @Provides
    @Singleton
    fun provideExportReportImageUseCase(
        @ApplicationContext context: Context
    ): ExportReportImageUseCase {
        return ExportReportImageUseCase(context)
    }
}
