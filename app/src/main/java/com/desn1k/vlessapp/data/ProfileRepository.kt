package com.desn1k.vlessapp.data

import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val dao: ProfileDao) {
    fun observeAll(): Flow<List<Profile>> = dao.observeAll()

    suspend fun getById(id: Long): Profile? = dao.getById(id)

    suspend fun save(profile: Profile): Long = dao.upsert(profile)

    suspend fun delete(profile: Profile) = dao.delete(profile)

    suspend fun recordLatency(id: Long, latencyMs: Long) =
        dao.updateLatency(id, latencyMs, System.currentTimeMillis())
}
