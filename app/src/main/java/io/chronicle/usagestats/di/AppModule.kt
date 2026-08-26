package io.chronicle.usagestats.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.chronicle.usagestats.core.util.AppIconHelper
import io.chronicle.usagestats.data.local.dao.AppOverrideDao
import io.chronicle.usagestats.data.local.dao.UsageDao
import io.chronicle.usagestats.data.local.preferences.UserPreferencesRepository
import io.chronicle.usagestats.data.repository.UsageStatsRepositoryImpl
import io.chronicle.usagestats.domain.repository.UsageRepository
import io.chronicle.usagestats.domain.usecase.CalculateStreaksUseCase
import io.chronicle.usagestats.domain.usecase.ExportCsvUseCase
import io.chronicle.usagestats.domain.usecase.ExportPdfReportUseCase
import io.chronicle.usagestats.domain.usecase.ExportReportImageUseCase
import io.chronicle.usagestats.domain.usecase.GetAppDetailUseCase
import io.chronicle.usagestats.domain.usecase.GetReportUseCase
import io.chronicle.usagestats.domain.usecase.GetTimelineUsageUseCase
import io.chronicle.usagestats.domain.usecase.SaveAppOverrideUseCase
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
        appOverrideDao: AppOverrideDao,
        appIconHelper: AppIconHelper
    ): UsageRepository {
        return UsageStatsRepositoryImpl(context, usageDao, appOverrideDao, appIconHelper)
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
    fun provideGetAppDetailUseCase(
        usageRepository: UsageRepository
    ): GetAppDetailUseCase {
        return GetAppDetailUseCase(usageRepository)
    }

    @Provides
    @Singleton
    fun provideCalculateStreaksUseCase(
        usageRepository: UsageRepository
    ): CalculateStreaksUseCase {
        return CalculateStreaksUseCase(usageRepository)
    }

    @Provides
    @Singleton
    fun provideSaveAppOverrideUseCase(
        usageRepository: UsageRepository
    ): SaveAppOverrideUseCase {
        return SaveAppOverrideUseCase(usageRepository)
    }

    @Provides
    @Singleton
    fun provideExportCsvUseCase(
        usageRepository: UsageRepository
    ): ExportCsvUseCase {
        return ExportCsvUseCase(usageRepository)
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

    @Provides
    @Singleton
    fun provideNetworkUsageRepository(
        dataSource: io.chronicle.usagestats.data.datasource.NetworkStatsDataSource,
        networkDao: io.chronicle.usagestats.data.local.dao.NetworkUsageDao,
        appOverrideDao: AppOverrideDao
    ): io.chronicle.usagestats.domain.repository.NetworkUsageRepository {
        return io.chronicle.usagestats.data.repository.NetworkUsageRepositoryImpl(dataSource, networkDao, appOverrideDao)
    }

    @Provides
    @Singleton
    fun provideGetDataUsageUseCase(
        repository: io.chronicle.usagestats.domain.repository.NetworkUsageRepository
    ): io.chronicle.usagestats.domain.usecase.GetDataUsageUseCase {
        return io.chronicle.usagestats.domain.usecase.GetDataUsageUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSyncDataUsageUseCase(
        repository: io.chronicle.usagestats.domain.repository.NetworkUsageRepository
    ): io.chronicle.usagestats.domain.usecase.SyncDataUsageUseCase {
        return io.chronicle.usagestats.domain.usecase.SyncDataUsageUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideExportDataPdfUseCase(
        @ApplicationContext context: Context
    ): io.chronicle.usagestats.domain.usecase.ExportDataPdfUseCase {
        return io.chronicle.usagestats.domain.usecase.ExportDataPdfUseCase(context)
    }

    @Provides
    @Singleton
    fun provideExportDataImageUseCase(
        @ApplicationContext context: Context
    ): io.chronicle.usagestats.domain.usecase.ExportDataImageUseCase {
        return io.chronicle.usagestats.domain.usecase.ExportDataImageUseCase(context)
    }

    @Provides
    @Singleton
    fun provideAppUpdateManager(
        @ApplicationContext context: Context
    ): io.chronicle.usagestats.core.updater.AppUpdateManager {
        return io.chronicle.usagestats.core.updater.AppUpdateManager(context)
    }

    @Provides
    @Singleton
    fun provideAppLimitRepository(
        dao: io.chronicle.usagestats.data.local.dao.AppLimitDao,
        @ApplicationContext context: Context
    ): io.chronicle.usagestats.domain.repository.AppLimitRepository {
        return io.chronicle.usagestats.data.repository.AppLimitRepositoryImpl(dao, context)
    }
}
