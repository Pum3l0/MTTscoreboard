package io.github.pum3l0.scoreboard.data.match

import io.github.pum3l0.scoreboard.domain.match.MatchAction
import io.github.pum3l0.scoreboard.domain.match.MatchConfig
import io.github.pum3l0.scoreboard.domain.match.MatchFormat
import io.github.pum3l0.scoreboard.domain.match.MatchReducer
import io.github.pum3l0.scoreboard.domain.match.MatchSnapshot
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomMatchStoreTest {
    @Test
    fun savedRowCanRestoreScoreAndUndoAndMoveToHistory() {
        runBlocking {
            val dao = MemoryDao()
            val store = RoomMatchStore(dao)
            val config = MatchConfig("Аня", "Борис", MatchFormat.BEST_OF_ONE, PlayerSide.SECOND)
            val start = MatchReducer.start(config)
            val firstPoint = MatchReducer.reduce(start, MatchAction.AwardPoint(PlayerSide.SECOND))
            var match = StoredMatch("match-id", firstPoint, startedAt = 123)
            store.create(match)

            assertEquals("2", dao.row?.points)
            val restored = checkNotNull(RoomMatchStore(dao).loadPending())
            assertEquals(match, restored)
            assertEquals(start, MatchReducer.reduce(restored.match, MatchAction.UndoLastPoint))

            repeat(11) {
                match = match.copy(match = MatchReducer.reduce(match.match, MatchAction.AwardPoint(PlayerSide.FIRST)))
            }
            val completed = match.copy(finishedAt = 456)
            store.save(restored, completed)
            assertNull(store.loadPending())
            assertEquals(listOf(completed), RoomMatchStore(dao).loadHistory())
        }
    }

    @Test
    fun importingHistoryTwiceDoesNotChangePendingMatchOrDuplicateResults() {
        runBlocking {
            val store = RoomMatchStore(MemoryDao())
            val config = MatchConfig("Аня", "Борис", MatchFormat.BEST_OF_ONE, PlayerSide.FIRST)
            val pending = StoredMatch("pending", MatchReducer.start(config), startedAt = 123)
            store.create(pending)
            val points = List(11) { PlayerSide.SECOND }
            val finished = MatchReducer.restore(MatchSnapshot(config, points))
            val archived = StoredMatch("archived", finished, startedAt = 10, finishedAt = 20)

            assertEquals(1, store.importHistory(listOf(archived)))
            assertEquals(0, store.importHistory(listOf(archived)))
            assertEquals(listOf(archived), store.loadHistory())
            assertEquals(pending, store.loadPending())
        }
    }

    @Test
    fun clearingHistoryRetainsPendingMatch() {
        runBlocking {
            val store = RoomMatchStore(MemoryDao())
            val config = MatchConfig("Аня", "Борис", MatchFormat.BEST_OF_ONE, PlayerSide.SECOND)
            val pending = StoredMatch("pending", MatchReducer.start(config), startedAt = 123)
            store.create(pending)
            val finished = MatchReducer.restore(MatchSnapshot(config, List(11) { PlayerSide.FIRST }))
            store.importHistory(listOf(StoredMatch("archived", finished, startedAt = 10, finishedAt = 20)))

            store.clearHistory()

            assertEquals(emptyList<StoredMatch>(), store.loadHistory())
            assertEquals(pending, store.loadPending())
        }
    }

    private class MemoryDao : MatchDao {
        private val rows = mutableMapOf<String, MatchEntity>()
        val row: MatchEntity?
            get() = rows.values.singleOrNull()

        override suspend fun pending() = rows.values.firstOrNull { it.pendingSlot == 1 }

        override suspend fun history() = rows.values.filter { it.finishedAt != null }

        override suspend fun clearHistory() {
            rows.entries.removeAll { it.value.finishedAt != null }
        }

        override suspend fun insertArchived(matches: List<MatchEntity>): List<Long> =
            matches.map {
                if (rows.putIfAbsent(it.id, it) == null) 1L else -1L
            }

        override suspend fun create(match: MatchEntity) {
            rows[match.id] = match
        }

        override suspend fun save(
            id: String,
            previousPoints: String,
            points: String,
            finishedAt: Long?,
            pendingSlot: Int?,
            previousSidesSwapped: Boolean,
            sidesSwapped: Boolean,
            previousTiming: String?,
            timing: String?,
        ): Int {
            val current = rows[id] ?: return 0
            if (current.id != id || current.points != previousPoints || current.pendingSlot != 1 || current.sidesSwapped != previousSidesSwapped || current.timing != previousTiming) return 0
            rows[id] = current.copy(points = points, finishedAt = finishedAt, pendingSlot = pendingSlot, sidesSwapped = sidesSwapped, timing = timing)
            return 1
        }

        override suspend fun discardPending(id: String): Int {
            if (rows[id]?.pendingSlot != 1) return 0
            rows.remove(id)
            return 1
        }
    }
}
