package com.desn1k.vlessapp.data

import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val dao: ProfileDao) {
    fun observeAll(): Flow<List<Profile>> = dao.observeAll()

    suspend fun getById(id: Long): Profile? = dao.getById(id)

    suspend fun save(profile: Profile): Long = dao.upsert(profile)

    suspend fun delete(profile: Profile) = dao.delete(profile)

    suspend fun recordLatency(id: Long, latencyMs: Long) =
        dao.updateLatency(id, latencyMs, System.currentTimeMillis())

    suspend fun getAllOnce(): List<Profile> = dao.getAllOnce()

    /** Inserts, or updates in place if a profile with the same address/port/uuid already exists. */
    suspend fun saveDeduped(profile: Profile): Long {
        val existing = dao.findByKey(profile.address, profile.port, profile.uuid)
        val toSave = if (existing != null) profile.copy(id = existing.id, createdAt = existing.createdAt) else profile
        return dao.upsert(toSave)
    }
}
