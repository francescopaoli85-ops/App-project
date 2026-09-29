package com.francescopaoli.northstar.data

import kotlinx.coroutines.flow.Flow

/** Sorgente dati degli obiettivi: Firestore (cloud, multi-dispositivo) o file locale. */
interface GoalRepository {
    fun observeGoals(): Flow<List<Goal>>
    suspend fun getGoals(): List<Goal>
    suspend fun upsert(goal: Goal)
    suspend fun delete(id: String)
}
