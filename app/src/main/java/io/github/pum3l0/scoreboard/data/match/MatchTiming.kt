package io.github.pum3l0.scoreboard.data.match

data class MatchTiming(
    val gamesMillis: List<Long> = emptyList(),
    val activeGameMillis: Long = 0,
) {
    init {
        require(gamesMillis.all { it >= 0 } && activeGameMillis >= 0)
    }

    val totalMillis: Long
        get() = gamesMillis.fold(activeGameMillis) { total, time -> Math.addExact(total, time) }

    fun atGameCount(completedGames: Int): MatchTiming = when (completedGames) {
        gamesMillis.size -> this
        gamesMillis.size + 1 -> MatchTiming(gamesMillis + activeGameMillis)
        gamesMillis.size - 1 -> MatchTiming(gamesMillis.dropLast(1), Math.addExact(gamesMillis.last(), activeGameMillis))
        else -> error("Unexpected change in game count")
    }
}

internal fun MatchTiming.encode(): String = (gamesMillis + activeGameMillis).joinToString(",")

internal fun decodeTiming(encoded: String, expectedGames: Int): MatchTiming {
    val values = encoded.split(',')
    require(values.size == expectedGames + 1)
    val valuesMillis = values.map(String::toLong)
    return MatchTiming(valuesMillis.dropLast(1), valuesMillis.last()).also { it.totalMillis }
}
