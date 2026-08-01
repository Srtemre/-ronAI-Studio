package com.htmltoapk.studio.core.di

import com.htmltoapk.studio.data.repository.ProjectRepositoryImpl
import com.htmltoapk.studio.data.repository.SettingsRepositoryImpl
import com.htmltoapk.studio.domain.repository.ProjectRepository
import com.htmltoapk.studio.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindProjectRepo(impl: ProjectRepositoryImpl): ProjectRepository
    @Binds @Singleton abstract fun bindSettingsRepo(impl: SettingsRepositoryImpl): SettingsRepository
}
