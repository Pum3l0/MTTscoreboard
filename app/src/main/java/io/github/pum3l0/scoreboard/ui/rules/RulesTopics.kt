package io.github.pum3l0.scoreboard.ui.rules

import io.github.pum3l0.scoreboard.R

internal data class RuleTopic(val title: Int, val keywords: Int, val reference: String, val entries: List<Pair<Int, Int>>)

internal val ruleTopics = listOf(
    RuleTopic(R.string.rule_score, R.string.rule_score_keywords, "ITTF 2.11–2.12", listOf(
        R.string.rule_score_game to R.string.rule_score_game_body,
        R.string.rule_score_match to R.string.rule_score_match_body,
    )),
    RuleTopic(R.string.rule_service_order, R.string.rule_service_order_keywords, "ITTF 2.13", listOf(
        R.string.rule_first_service to R.string.rule_first_service_body,
        R.string.rule_service_rotation to R.string.rule_service_rotation_body,
    )),
    RuleTopic(R.string.rule_serve, R.string.rule_serve_keywords, "ITTF 2.6", listOf(
        R.string.rule_toss to R.string.rule_toss_body,
        R.string.rule_serve_bounce to R.string.rule_serve_bounce_body,
    )),
    RuleTopic(R.string.rule_let, R.string.rule_let_keywords, "ITTF 2.7, 2.9", listOf(
        R.string.rule_net_on_serve to R.string.rule_net_on_serve_body,
        R.string.rule_interruptions to R.string.rule_interruptions_body,
    )),
    RuleTopic(R.string.rule_rally, R.string.rule_rally_keywords, "ITTF 2.1, 2.5, 2.7, 2.10", listOf(
        R.string.rule_point to R.string.rule_point_body,
        R.string.rule_edge to R.string.rule_edge_body,
        R.string.rule_contact to R.string.rule_contact_body,
    )),
    RuleTopic(R.string.rule_ends, R.string.rule_ends_keywords, "ITTF 2.13.7", listOf(
        R.string.rule_change_ends to R.string.rule_change_ends_body,
        R.string.rule_final_game to R.string.rule_final_game_body,
    )),
    RuleTopic(R.string.rule_doubles, R.string.rule_doubles_keywords, "ITTF 2.6.3, 2.8.2, 2.13.4–2.13.6", listOf(
        R.string.rule_doubles_serve to R.string.rule_doubles_serve_body,
        R.string.rule_doubles_turns to R.string.rule_doubles_turns_body,
    )),
)
