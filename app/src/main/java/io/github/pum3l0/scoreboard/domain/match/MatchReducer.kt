package io.github.pum3l0.scoreboard.domain.match

object MatchReducer {
    fun start(config: MatchConfig): MatchState = MatchState(config)

    fun reduce(state: MatchState, action: MatchAction): MatchState = when (action) {
        is MatchAction.AwardPoint -> {
            if (state.winner == null) {
                advance(state, action.player).copy(pointWinners = state.pointWinners + action.player)
            } else {
                state
            }
        }

        MatchAction.UndoLastPoint -> {
            if (state.canUndo) {
                restore(MatchSnapshot(state.config, state.pointWinners.dropLast(1)))
            } else {
                state
            }
        }
    }

    fun restore(snapshot: MatchSnapshot): MatchState {
        val pointWinners = snapshot.pointWinners.toList()
        val state = pointWinners.fold(start(snapshot.config)) { match, player ->
            require(match.winner == null) { "Snapshot contains points after the match ended" }
            advance(match, player)
        }
        return state.copy(pointWinners = pointWinners)
    }

    private fun advance(state: MatchState, player: PlayerSide): MatchState {
        val score = checkNotNull(state.activeGameScore).withPointFor(player)
        val gameWon = when (player) {
            PlayerSide.FIRST -> score.first >= 11 && score.first - score.second >= 2
            PlayerSide.SECOND -> score.second >= 11 && score.second - score.first >= 2
        }

        if (!gameWon) return state.copy(activeGameScore = score)

        val gamesWon = state.gamesWon.withPointFor(player)
        val matchWon = maxOf(gamesWon.first, gamesWon.second) == state.config.format.gamesToWin
        return state.copy(
            activeGameScore = if (matchWon) null else Score(),
            completedGames = state.completedGames + GameResult(score, player),
            gamesWon = gamesWon,
            winner = if (matchWon) player else null,
        )
    }
}
