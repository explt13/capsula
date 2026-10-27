package com.capsula.app.data.repository

import com.capsula.app.data.model.Capsule
import com.capsula.app.data.model.Mood
import com.capsula.app.data.model.NewCapsule
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for capsules. ViewModels depend on this interface only, so the
 * in-memory implementation can be swapped for Room in part 2 without touching the UI.
 * Write operations are `suspend` for the same reason: a database call is asynchronous.
 */
interface CapsuleRepository {
    val capsules: Flow<List<Capsule>>

    fun observeCapsule(id: Long): Flow<Capsule?>

    suspend fun addCapsule(newCapsule: NewCapsule): Long

    suspend fun markOpened(id: Long, openedAt: Long)

    suspend fun saveReflection(id: Long, mood: Mood, reflection: String)

    suspend fun deleteCapsule(id: Long)
}
