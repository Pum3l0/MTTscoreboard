package io.github.pum3l0.scoreboard.ui.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleSearchTest {
    @Test
    fun searchUsesAllTermsAndNormalizesRussianLettersAndSeparators() {
        val fields = listOf("Порядок подачи", "Счёт 10:10, меняется очерёдность")
        assertTrue(matchesRuleQuery("счет 10–10", fields))
        assertTrue(matchesRuleQuery("  ПОДАЧИ, очередность ", fields))
        assertTrue(matchesRuleQuery("   ", fields))
        assertFalse(matchesRuleQuery("подача ракетка", fields))
    }
}
