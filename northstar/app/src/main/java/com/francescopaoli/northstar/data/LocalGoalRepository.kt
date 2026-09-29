package com.francescopaoli.northstar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Salvataggio su file JSON nel telefono. Usato quando Firebase non è configurato. */
class LocalGoalRepository(context: Context) : GoalRepository {

    private val file = File(context.filesDir, "goals.json")
    private val mutex = Mutex()
    private val state = MutableStateFlow(load())

    override fun observeGoals(): Flow<List<Goal>> = state

    override suspend fun getGoals(): List<Goal> = state.value

    override suspend fun upsert(goal: Goal) = mutate { list ->
        list.filterNot { it.id == goal.id } + goal
    }

    override suspend fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    private suspend fun mutate(block: (List<Goal>) -> List<Goal>) = mutex.withLock {
        val updated = block(state.value)
        state.value = updated
        withContext(Dispatchers.IO) { save(updated) }
    }

    private fun load(): List<Goal> = runCatching {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        (0 until arr.length()).mapNotNull { GoalMapper.fromMap(arr.getJSONObject(it).toMap()) }
    }.getOrDefault(emptyList())

    private fun save(goals: List<Goal>) {
        val arr = JSONArray()
        goals.forEach { arr.put(JSONObject(GoalMapper.toMap(it))) }
        file.writeText(arr.toString())
    }
}

/** JSONObject -> Map ricorsivo (liste e oggetti annidati inclusi). */
private fun JSONObject.toMap(): Map<String, Any?> =
    keys().asSequence().associateWith { key -> unwrap(get(key)) }

private fun unwrap(v: Any?): Any? = when (v) {
    JSONObject.NULL -> null
    is JSONObject -> v.toMap()
    is JSONArray -> (0 until v.length()).map { unwrap(v.get(it)) }
    else -> v
}
