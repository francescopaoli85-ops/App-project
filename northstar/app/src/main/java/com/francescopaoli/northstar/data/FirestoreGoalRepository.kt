package com.francescopaoli.northstar.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Obiettivi su Firestore in users/{uid}/goals/{goalId}.
 * Firestore tiene una cache offline: l'app funziona anche senza rete
 * e si sincronizza da sola su tutti i dispositivi dello stesso account.
 */
class FirestoreGoalRepository(
    private val db: FirebaseFirestore,
    private val uid: String,
) : GoalRepository {

    private val col get() = db.collection("users").document(uid).collection("goals")

    override fun observeGoals(): Flow<List<Goal>> = callbackFlow {
        val reg = col.addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.documents.mapNotNull { d -> d.data?.let(GoalMapper::fromMap) })
        }
        awaitClose { reg.remove() }
    }

    override suspend fun getGoals(): List<Goal> =
        col.get().await().documents.mapNotNull { d -> d.data?.let(GoalMapper::fromMap) }

    override suspend fun upsert(goal: Goal) {
        // Niente await: con la cache offline la scrittura è immediata in locale,
        // e aspettare il server bloccherebbe la UI quando si è senza rete.
        col.document(goal.id).set(GoalMapper.toMap(goal))
    }

    override suspend fun delete(id: String) {
        col.document(id).delete()
    }
}
