package io.github.pum3l0.scoreboard.domain.match

enum class PlayerSide {
    FIRST,
    SECOND;

    val opponent: PlayerSide
        get() = when (this) {
            FIRST -> SECOND
            SECOND -> FIRST
        }
}

enum class MatchFormat(val gamesToWin: Int) {
    BEST_OF_ONE(1),
    BEST_OF_THREE(2),
    BEST_OF_FIVE(3),
    BEST_OF_SEVEN(4),
}

data class MatchConfig(
    val firstPlayerName: String,
    val secondPlayerName: String,
    val format: MatchFormat,
    val firstServer: PlayerSide,
) {
    init {
        require(firstPlayerName.isNotBlank() && secondPlayerName.isNotBlank()) {
            "Both players must have a name"
        }
    }
}

data class Score(val first: Int = 0, val second: Int = 0) {
    internal fun withPointFor(player: PlayerSide): Score = when (player) {
        PlayerSide.FIRST -> copy(first = first + 1)
        PlayerSide.SECOND -> copy(second = second + 1)
    }
}

@ConsistentCopyVisibility
data class GameResult internal constructor(val score: Score, val winner: PlayerSide)

/** Complete input for restoring the match, including the information needed to undo points. */
data class MatchSnapshot(val config: MatchConfig, val pointWinners: List<PlayerSide>)

@ConsistentCopyVisibility
data class MatchState internal constructor(
    val config: MatchConfig,
    val activeGameScore: Score? = Score(),
    val completedGames: List<GameResult> = emptyList(),
    val gamesWon: Score = Score(),
    val winner: PlayerSide? = null,
    internal val pointWinners: List<PlayerSide> = emptyList(),
) {
    val canUndo: Boolean
        get() = pointWinners.isNotEmpty()

    val server: PlayerSide?
        get() {
            val score = activeGameScore ?: return null
            val firstServer = if (completedGames.size % 2 == 0) {
                config.firstServer
            } else {
                config.firstServer.opponent
            }
            val totalPoints = score.first.toLong() + score.second
            val serviceTurns = if (score.first >= 10 && score.second >= 10) {
                10 + (totalPoints - 20)
            } else {
                totalPoints / 2
            }
            return if (serviceTurns % 2 == 0L) firstServer else firstServer.opponent
        }

    fun snapshot(): MatchSnapshot = MatchSnapshot(config, pointWinners.toList())
}

sealed interface MatchAction {
    data class AwardPoint(val player: PlayerSide) : MatchAction
    data object UndoLastPoint : MatchAction
}
