package io.github.pum3l0.scoreboard.domain.match

import io.github.pum3l0.scoreboard.domain.match.MatchAction.AwardPoint
import io.github.pum3l0.scoreboard.domain.match.MatchAction.UndoLastPoint
import io.github.pum3l0.scoreboard.domain.match.PlayerSide.FIRST
import io.github.pum3l0.scoreboard.domain.match.PlayerSide.SECOND
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class MatchReducerTest {
    private val config = MatchConfig("Анна", "Борис", MatchFormat.BEST_OF_THREE, SECOND)

    @Test
    fun initialServerSwitchesEveryTwoPoints() {
        val start = MatchReducer.start(config)
        assertEquals(SECOND, start.server)
        assertEquals(SECOND, start.award(FIRST).server)
        assertEquals(FIRST, start.award(FIRST, SECOND).server)
        assertEquals(SECOND, start.award(FIRST, SECOND, SECOND, SECOND).server)
    }

    @Test
    fun deuceSwitchesServerEveryPointAndRequiresTwoPointLead() {
        val atTenNine = MatchReducer.start(config).award(*tiedPoints(9), FIRST)
        val deuce = atTenNine.award(SECOND)
        val advantage = deuce.award(FIRST)
        val secondDeuce = advantage.award(SECOND)
        val nextGame = secondDeuce.award(SECOND, SECOND)

        assertEquals(FIRST, atTenNine.server)
        assertEquals(SECOND, deuce.server)
        assertEquals(Score(11, 10), advantage.activeGameScore)
        assertEquals(FIRST, advantage.server)
        assertEquals(Score(11, 11), secondDeuce.activeGameScore)
        assertEquals(SECOND, secondDeuce.server)
        assertEquals(listOf(GameResult(Score(11, 13), SECOND)), nextGame.completedGames)
        assertEquals(FIRST, nextGame.server)
    }

    @Test
    fun nextGameStartsAtZeroWithOppositeFirstServer() {
        val gameTwo = MatchReducer.start(config).winGame(FIRST)
        val gameThree = gameTwo.winGame(SECOND)

        assertEquals(Score(), gameTwo.activeGameScore)
        assertEquals(Score(1, 0), gameTwo.gamesWon)
        assertEquals(Score(11, 0), gameTwo.completedGames.single().score)
        assertEquals(FIRST, gameTwo.server)
        assertEquals(Score(1, 1), gameThree.gamesWon)
        assertEquals(SECOND, gameThree.server)
    }

    @Test
    fun eachFormatEndsAtItsRequiredNumberOfGameWins() {
        for (format in MatchFormat.entries) {
            var state = MatchReducer.start(config.copy(format = format))
            repeat(format.gamesToWin - 1) {
                state = state.winGame(FIRST).winGame(SECOND)
                assertNull(state.winner)
            }
            state = state.winGame(SECOND)

            assertEquals(SECOND, state.winner)
            assertEquals(Score(format.gamesToWin - 1, format.gamesToWin), state.gamesWon)
            assertNull(state.activeGameScore)
            assertNull(state.server)
            assertSame(state, state.award(FIRST))
        }
    }

    @Test
    fun undoRestoresScoreAndServerAcrossGameAndMatchEnd() {
        val start = MatchReducer.start(config)
        val gamePoint = start.award(*Array(10) { FIRST })
        val nextGame = gamePoint.award(FIRST)
        val matchPoint = nextGame.award(*tiedPoints(10), FIRST)
        val finished = matchPoint.award(FIRST)

        assertFalse(start.canUndo)
        assertSame(start, MatchReducer.reduce(start, UndoLastPoint))
        assertEquals(gamePoint, MatchReducer.reduce(nextGame, UndoLastPoint))
        assertEquals(FIRST, finished.winner)
        assertEquals(matchPoint, MatchReducer.reduce(finished, UndoLastPoint))
    }

    @Test
    fun snapshotRestoresAnUnfinishedMatchAndUndoHistory() {
        val state = MatchReducer.start(config).winGame(SECOND).award(*tiedPoints(11), FIRST)
        val snapshot = state.snapshot()
        val restored = MatchReducer.restore(snapshot)

        assertEquals(state, restored)
        assertEquals(state.server, restored.server)
        assertTrue(restored.canUndo)
        assertEquals(
            MatchReducer.reduce(state, UndoLastPoint),
            MatchReducer.reduce(restored, UndoLastPoint),
        )
    }

    @Test
    fun snapshotRejectsPointsAfterTheMatchEnds() {
        val config = config.copy(format = MatchFormat.BEST_OF_ONE)
        val finished = MatchReducer.start(config).winGame(FIRST)
        assertEquals(finished, MatchReducer.restore(finished.snapshot()))
        val invalid = MatchSnapshot(config, finished.snapshot().pointWinners + SECOND)

        assertThrows(IllegalArgumentException::class.java) { MatchReducer.restore(invalid) }
    }

    private fun MatchState.award(vararg players: PlayerSide): MatchState =
        players.fold(this) { state, player -> MatchReducer.reduce(state, AwardPoint(player)) }

    private fun MatchState.winGame(player: PlayerSide): MatchState =
        award(*Array(11) { player })

    private fun tiedPoints(score: Int): Array<PlayerSide> =
        Array(score * 2) { if (it % 2 == 0) FIRST else SECOND }
}
