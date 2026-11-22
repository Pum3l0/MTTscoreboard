package io.github.pum3l0.scoreboard.ui.rules

private val searchToken = Regex("[\\p{L}\\p{N}]+")

internal fun matchesRuleQuery(query: String, fields: List<String>): Boolean {
    val terms = searchToken.findAll(query.searchForm()).map { it.value }.toList()
    if (terms.isEmpty()) return true
    val index = fields.joinToString(" ").searchForm()
    return terms.all { term -> term in index }
}

private fun String.searchForm(): String = lowercase().replace('ё', 'е')
