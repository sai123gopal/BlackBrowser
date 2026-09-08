package com.saigopal.browser.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * P09 (9.8): every flag combination of the exit-clear decision, mirroring the legacy
 * ClearService truth table (four independent sp_clear_* switches).
 */
class ClearPlannerTest {

    @Test
    fun `all flags off plans nothing`() {
        val flags = ClearPlanner.Flags(cache = false, cookies = false, history = false, indexedDb = false)
        assertTrue(ClearPlanner.plan(flags).isEmpty())
    }

    @Test
    fun `each single flag maps to exactly its op`() {
        assertEquals(
            listOf(ClearPlanner.Op.ClearCache),
            ClearPlanner.plan(ClearPlanner.Flags(cache = true)),
        )
        assertEquals(
            listOf(ClearPlanner.Op.ClearCookies),
            ClearPlanner.plan(ClearPlanner.Flags(cookies = true)),
        )
        assertEquals(
            listOf(ClearPlanner.Op.ClearHistory),
            ClearPlanner.plan(ClearPlanner.Flags(history = true)),
        )
        assertEquals(
            listOf(ClearPlanner.Op.ClearIndexedDb),
            ClearPlanner.plan(ClearPlanner.Flags(indexedDb = true)),
        )
    }

    @Test
    fun `legacy default all-on combination plans every op`() {
        val ops = ClearPlanner.plan(
            ClearPlanner.Flags(cache = true, cookies = true, history = true, indexedDb = true),
        )
        assertEquals(4, ops.size)
        assertEquals(setOf(ClearPlanner.Op.ClearCache, ClearPlanner.Op.ClearCookies, ClearPlanner.Op.ClearHistory, ClearPlanner.Op.ClearIndexedDb), ops.toSet())
    }

    @Test
    fun `plan is deterministic across calls`() {
        val flags = ClearPlanner.Flags(cache = true, cookies = false, history = true, indexedDb = true)
        assertEquals(ClearPlanner.plan(flags), ClearPlanner.plan(flags))
    }
}
