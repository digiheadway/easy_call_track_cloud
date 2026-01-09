package com.miniclick.calltrackmanage.di

import android.content.Context
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.util.network.NetworkConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Main Hilt DI module providing application-level dependencies.
 * 
 * This module consolidates all repository and utility class instantiation,
 * replacing manual singleton patterns with proper DI.
 * 
 * Benefits:
 * 1. Testability - Easy to provide mock implementations
 * 2. Consistency - Single source of truth for dependency creation
 * 3. Lifecycle management - Proper scoping of dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the CallDataRepository singleton.
     * Previously: CallDataRepository.getInstance(context)
     */
    @Provides
    @Singleton
    fun provideCallDataRepository(
        @ApplicationContext context: Context
    ): CallDataRepository {
        return CallDataRepository.getInstance(context)
    }

    /**
     * Provides the RecordingRepository singleton.
     * Previously: RecordingRepository.getInstance(context)
     */
    @Provides
    @Singleton
    fun provideRecordingRepository(
        @ApplicationContext context: Context
    ): RecordingRepository {
        return RecordingRepository.getInstance(context)
    }

    /**
     * Provides the SettingsRepository singleton.
     * Previously: SettingsRepository.getInstance(context)
     */
    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository.getInstance(context)
    }

    /**
     * Provides the NetworkConnectivityObserver.
     * Previously: NetworkConnectivityObserver(context)
     */
    @Provides
    @Singleton
    fun provideNetworkConnectivityObserver(
        @ApplicationContext context: Context
    ): NetworkConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }
}
