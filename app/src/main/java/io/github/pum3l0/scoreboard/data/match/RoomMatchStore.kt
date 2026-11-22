package io.github.pum3l0.scoreboard.data.match

import io.github.pum3l0.scoreboard.domain.match.MatchConfig
import io.github.pum3l0.scoreboard.domain.match.MatchFormat
import io.github.pum3l0.scoreboard.domain.match.MatchReducer
import io.github.pum3l0.scoreboard.domain.match.MatchSnapshot

class RoomMatchStore(private val dao: MatchDao) : MatchStore {
    override suspend fun loadPending(): StoredMatch? = dao.pending()?.toStoredMatch()

    override suspend fun loadHistory(): List<StoredMatch> = dao.history().map { it.toStoredMatch() }

    override suspend fun clearHistory() = dao.clearHistory()

    override suspend fun importHistory(matches: List<StoredMatch>): Int {
        val rows = matches.map {
            require(it.finishedAt != null && it.match.winner != null)
            it.toEntity()
        }
        return dao.insertArchived(rows).count { it != -1L }
    }

    override suspend fun create(match: StoredMatch, replacingId: String?) {
        check(match.finishedAt == null)
        val row = match.toEntity()
        if (replacingId == null) dao.create(row) else dao.replacePending(replacingId, row)
    }

    private fun StoredMatch.toEntity(): MatchEntity {
        val snapshot = match.snapshot()
        return MatchEntity(
            id = id,
            firstName = snapshot.config.firstPlayerName,
            secondName = snapshot.config.secondPlayerName,
            gamesToWin = snapshot.config.format.gamesToWin,
            firstServer = snapshot.config.firstServer.storageCode,
            points = snapshot.encodedPoints(),
            startedAt = startedAt,
            finishedAt = finishedAt,
            pendingSlot = if (finishedAt == null) 1 else null,
            sidesSwapped = sidesSwapped,
            timing = timing?.encode(),
        )
    }

    override suspend fun save(previous: StoredMatch, match: StoredMatch) {
        check(previous.id == match.id && previous.finishedAt == null)
        check(match.finishedAt == null || match.match.winner != null)
        val updated = dao.save(
            id = match.id,
            previousPoints = previous.match.snapshot().encodedPoints(),
            points = match.match.snapshot().encodedPoints(),
            finishedAt = match.finishedAt,
            pendingSlot = if (match.finishedAt == null) 1 else null,
            previousSidesSwapped = previous.sidesSwapped,
            sidesSwapped = match.sidesSwapped,
            previousTiming = previous.timing?.encode(),
            timing = match.timing?.encode(),
        )
        check(updated == 1) { "The stored match changed before this update" }
    }

    override suspend fun discardPending(id: String) {
        check(dao.discardPending(id) == 1) { "The pending match has already changed" }
    }

    private fun MatchEntity.toStoredMatch(): StoredMatch {
        val config = MatchConfig(
            firstPlayerName = firstName,
            secondPlayerName = secondName,
            format = MatchFormat.entries.first { it.gamesToWin == gamesToWin },
            firstServer = playerFromCode(firstServer),
        )
        val match = MatchReducer.restore(
            MatchSnapshot(config, decodePoints(points)),
        )
        check(finishedAt == null || match.winner != null)
        return StoredMatch(
            id, match, startedAt, finishedAt, sidesSwapped,
            timing?.let { decodeTiming(it, match.completedGames.size) },
        )
    }

}
