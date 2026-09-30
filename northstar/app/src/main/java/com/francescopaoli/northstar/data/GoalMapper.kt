package com.francescopaoli.northstar.data

/**
 * Conversione Goal <-> Map. La stessa mappa va bene sia per Firestore
 * sia per il file JSON locale, così c'è un solo formato da mantenere.
 */
object GoalMapper {

    fun toMap(g: Goal): Map<String, Any?> = mapOf(
        "id" to g.id,
        "area" to g.area.name,
        "answers" to g.answers.mapKeys { it.key.name },
        "summary" to g.summary,
        "deadlineEpochDay" to g.deadlineEpochDay,
        "status" to g.status.name,
        "actions" to g.actions.map { mapOf("id" to it.id, "text" to it.text, "done" to it.done, "doneAt" to it.doneAt) },
        "createdAt" to g.createdAt,
        "achievedAt" to g.achievedAt,
        "postponedCount" to g.postponedCount,
        "nextCheckinIndex" to g.nextCheckinIndex,
        "lastCheckinAt" to g.lastCheckinAt,
        "calendarEventId" to g.calendarEventId,
    )

    fun fromMap(m: Map<String, Any?>): Goal? = runCatching {
        @Suppress("UNCHECKED_CAST")
        val answers = (m["answers"] as? Map<String, Any?>).orEmpty()
            .mapNotNull { (k, v) -> enumOrNull<Criterion>(k)?.let { it to v.toString() } }
            .toMap()

        @Suppress("UNCHECKED_CAST")
        val actions = (m["actions"] as? List<Map<String, Any?>>).orEmpty().map {
            GoalAction(
                id = it["id"].toString(),
                text = it["text"].toString(),
                done = it["done"] as? Boolean ?: false,
                doneAt = (it["doneAt"] as? Number)?.toLong(),
            )
        }
        Goal(
            id = m["id"].toString(),
            area = enumOrNull<Area>(m["area"]) ?: Area.PERSONALE,
            answers = answers,
            summary = m["summary"]?.toString().orEmpty(),
            deadlineEpochDay = (m["deadlineEpochDay"] as Number).toLong(),
            status = enumOrNull<GoalStatus>(m["status"]) ?: GoalStatus.ACTIVE,
            actions = actions,
            createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0L,
            achievedAt = (m["achievedAt"] as? Number)?.toLong(),
            postponedCount = (m["postponedCount"] as? Number)?.toInt() ?: 0,
            nextCheckinIndex = (m["nextCheckinIndex"] as? Number)?.toInt() ?: 0,
            lastCheckinAt = (m["lastCheckinAt"] as? Number)?.toLong(),
            calendarEventId = m["calendarEventId"] as? String,
        )
    }.getOrNull()

    private inline fun <reified E : Enum<E>> enumOrNull(v: Any?): E? =
        enumValues<E>().firstOrNull { it.name == v?.toString() }
}
