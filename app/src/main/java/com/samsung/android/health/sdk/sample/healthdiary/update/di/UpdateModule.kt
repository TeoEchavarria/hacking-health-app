package com.samsung.android.health.sdk.sample.healthdiary.update.di

import android.content.Context
import com.samsung.android.health.sdk.sample.healthdiary.api.RetrofitClient
import com.samsung.android.health.sdk.sample.healthdiary.update.data.api.GitHubReleaseApi
import com.samsung.android.health.sdk.sample.healthdiary.update.data.repository.UpdateRepository
import com.samsung.android.health.sdk.sample.healthdiary.update.data.repository.UpdateRepositoryImpl
import com.samsung.android.health.sdk.sample.healthdiary.update.manager.AppUpdateManager
import com.samsung.android.health.sdk.sample.healthdiary.update.manager.AppUpdateManagerImpl
import com.samsung.android.health.sdk.sample.healthdiary.update.manager.WearUpdateManager
import com.samsung.android.health.sdk.sample.healthdiary.update.manager.WearUpdateManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {

    @Provides
    @Singleton
    fun provideGitHubReleaseApi(): GitHubReleaseApi {
        return RetrofitClient.gitHubReleaseApi
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(api: GitHubReleaseApi): UpdateRepository {
        return UpdateRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideAppUpdateManager(@ApplicationContext context: Context): AppUpdateManager {
        return AppUpdateManagerImpl(context)
    }

    @Provides
    @Singleton
    fun provideWearUpdateManager(@ApplicationContext context: Context): WearUpdateManager {
        return WearUpdateManagerImpl(context)
    }
}
