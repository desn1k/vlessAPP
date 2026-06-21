package com.desn1k.vlessapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: Profile): Long

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)

    @Query("UPDATE profiles SET lastLatencyMs = :latencyMs, lastCheckedAt = :checkedAt WHERE id = :id")
    suspend fun updateLatency(id: Long, latencyMs: Long, checkedAt: Long)

    @Query("SELECT * FROM profiles WHERE address = :address AND port = :port AND uuid = :uuid LIMIT 1")
    suspend fun findByKey(address: String, port: Int, uuid: String): Profile?

    @Query("SELECT * FROM profiles")
    suspend fun getAllOnce(): List<Profile>
}
