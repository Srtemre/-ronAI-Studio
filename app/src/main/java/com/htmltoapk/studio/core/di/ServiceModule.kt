package com.htmltoapk.studio.core.di

import com.htmltoapk.studio.builder.ApkBuilder
import com.htmltoapk.studio.builder.ApkBuilderImpl
import com.htmltoapk.studio.generator.ProjectGenerator
import com.htmltoapk.studio.generator.ProjectGeneratorImpl
import com.htmltoapk.studio.importer.SourceImporter
import com.htmltoapk.studio.importer.SourceImporterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds @Singleton abstract fun bindGenerator(impl: ProjectGeneratorImpl): ProjectGenerator
    @Binds @Singleton abstract fun bindImporter(impl: SourceImporterImpl): SourceImporter
    @Binds @Singleton abstract fun bindBuilder(impl: ApkBuilderImpl): ApkBuilder
}
