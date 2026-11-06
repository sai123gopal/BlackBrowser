package com.saigopal.browser.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserHistoryCompletionGateTest {
    @Test
    fun `only first completion is accepted for a navigation`() {
        val gate = BrowserHistoryCompletionGate()

        assertTrue(gate.completeOnce())
        assertFalse(gate.completeOnce())

        gate.beginNavigation()

        assertTrue(gate.completeOnce())
        assertFalse(gate.completeOnce())
    }
}