package io.github.pum3l0.scoreboard.data.match

import io.github.pum3l0.scoreboard.domain.match.MatchState

data class StoredMatch(
    val id: String,
    val match: MatchState,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val sidesSwapped: Boolean = false,
    val timing: MatchTiming? = null,
)

interface MatchStore {
    suspend fun loadPending(): StoredMatch?
    suspend fun loadHistory(): List<StoredMatch>
    suspend fun clearHistory()
    suspend fun importHistory(matches: List<StoredMatch>): Int
    suspend fun create(match: StoredMatch, replacingId: String? = null)
    suspend fun save(previous: StoredMatch, match: StoredMatch)
    suspend fun discardPending(id: String)
}
