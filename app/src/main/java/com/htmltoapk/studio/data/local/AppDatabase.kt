package com.htmltoapk.studio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.htmltoapk.studio.data.local.converter.Converters
import com.htmltoapk.studio.data.local.dao.ProjectDao
import com.htmltoapk.studio.data.local.entity.ProjectEntity

@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        const val NAME = "htmltoapk.db"
    }
}
