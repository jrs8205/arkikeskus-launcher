package org.arkikeskus.launcher.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeItemDao {
    @Query("SELECT * FROM home_items ORDER BY page ASC, cellY ASC, cellX ASC")
    fun observeAll(): Flow<List<HomeItemEntity>>

    @Query("SELECT * FROM home_items ORDER BY page ASC, cellY ASC, cellX ASC")
    suspend fun getAllOrdered(): List<HomeItemEntity>

    @Query("SELECT * FROM home_items")
    suspend fun getAll(): List<HomeItemEntity>

    @Query(
        "SELECT * FROM home_items WHERE packageName = :pkg AND className = :cls AND userSerial = :user LIMIT 1",
    )
    suspend fun getByKey(pkg: String, cls: String, user: Long): HomeItemEntity?

    @Query("SELECT * FROM home_items WHERE page = :page AND cellX = :cellX AND cellY = :cellY LIMIT 1")
    suspend fun getAt(page: Int, cellX: Int, cellY: Int): HomeItemEntity?

    @Insert
    suspend fun insert(item: HomeItemEntity)

    @Query("DELETE FROM home_items WHERE packageName = :pkg AND className = :cls AND userSerial = :user")
    suspend fun deleteByKey(pkg: String, cls: String, user: Long)

    @Query("DELETE FROM home_items")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM home_items WHERE packageName = :pkg AND className = :cls AND userSerial = :user")
    suspend fun count(pkg: String, cls: String, user: Long): Int

    @Query("UPDATE home_items SET page = :page, cellX = :cellX, cellY = :cellY WHERE id = :id")
    suspend fun moveById(id: Long, page: Int, cellX: Int, cellY: Int)
}
