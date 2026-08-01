package com.htmltoapk.studio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.htmltoapk.studio.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY modifiedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE favorite = 1 ORDER BY modifiedAt DESC")
    fun observeFavorites(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY modifiedAt DESC LIMIT 5")
    fun observeRecent(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE appName LIKE '%' || :q || '%' OR packageName LIKE '%' || :q || '%' ORDER BY modifiedAt DESC")
    fun search(q: String): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM projects WHERE favorite = 1")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT MAX(lastBuiltAt) FROM projects")
    fun observeLastBuild(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProjectEntity): Long

    @Update
    suspend fun update(entity: ProjectEntity)

    @Delete
    suspend fun delete(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE projects SET favorite = :fav, modifiedAt = :ts WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean, ts: Long)

    @Query("UPDATE projects SET appName = :name, modifiedAt = :ts WHERE id = :id")
    suspend fun rename(id: Long, name: String, ts: Long)

    @Query("UPDATE projects SET lastBuiltAt = :ts, generatedPath = :path WHERE id = :id")
    suspend fun markBuilt(id: Long, ts: Long, path: String)
}
