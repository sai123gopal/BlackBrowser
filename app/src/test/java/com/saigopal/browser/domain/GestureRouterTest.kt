package com.saigopal.browser.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Locks the gesture preference-code table (legacy values "01".."10") and the dispatch rule
 * that "01" (none) falls back to normal click handling.
 */
class GestureRouterTest {

    private class FakeCodes(
        val up: String? = null, val down: String? = null,
        val left: String? = null, val right: String? = null,
    ) : GestureCodes {
        override fun upCode(area: GestureArea) = up
        override fun downCode(area: GestureArea) = down
        override fun leftCode(area: GestureArea) = left
        override fun rightCode(area: GestureArea) = right
    }

    @Test fun `codes map to actions exactly like the legacy switch`() {
        assertEquals(GestureAction.NONE, GestureAction.fromCode("01"))
        assertEquals(GestureAction.FORWARD, GestureAction.fromCode("02"))
        assertEquals(GestureAction.BACK, GestureAction.fromCode("03"))
        assertEquals(GestureAction.PAGE_UP, GestureAction.fromCode("04"))
        assertEquals(GestureAction.PAGE_DOWN, GestureAction.fromCode("05"))
        assertEquals(GestureAction.NEXT_TAB, GestureAction.fromCode("06"))
        assertEquals(GestureAction.PREVIOUS_TAB, GestureAction.fromCode("07"))
        assertEquals(GestureAction.OPEN_START_OVERVIEW, GestureAction.fromCode("08"))
        assertEquals(GestureAction.NEW_TAB, GestureAction.fromCode("09"))
        assertEquals(GestureAction.CLOSE_TAB, GestureAction.fromCode("10"))
    }

    @Test fun `unknown or missing code resolves to none`() {
        assertEquals(GestureAction.NONE, GestureAction.fromCode(null))
        assertEquals(GestureAction.NONE, GestureAction.fromCode("99"))
        assertEquals(GestureAction.NONE, GestureAction.fromCode(""))
    }

    @Test fun `first-run defaults resolve per area`() {
        val codes = FakeCodes(up = "08", down = "01", left = "07", right = "06")
        assertEquals(GestureAction.OPEN_START_OVERVIEW, GestureRouter.actionFor(codes, GestureArea.TOOLBAR, GestureDirection.UP))
        assertEquals(GestureAction.NONE, GestureRouter.actionFor(codes, GestureArea.TOOLBAR, GestureDirection.DOWN))
        assertEquals(GestureAction.PREVIOUS_TAB, GestureRouter.actionFor(codes, GestureArea.TOOLBAR, GestureDirection.LEFT))
        assertEquals(GestureAction.NEXT_TAB, GestureRouter.actionFor(codes, GestureArea.TOOLBAR, GestureDirection.RIGHT))
    }

    @Test fun `nav button defaults are paging and navigation`() {
        val codes = FakeCodes(up = "04", down = "05", left = "03", right = "02")
        assertEquals(GestureAction.PAGE_UP, GestureRouter.actionFor(codes, GestureArea.NAV_BUTTON, GestureDirection.UP))
        assertEquals(GestureAction.PAGE_DOWN, GestureRouter.actionFor(codes, GestureArea.NAV_BUTTON, GestureDirection.DOWN))
        assertEquals(GestureAction.BACK, GestureRouter.actionFor(codes, GestureArea.NAV_BUTTON, GestureDirection.LEFT))
        assertEquals(GestureAction.FORWARD, GestureRouter.actionFor(codes, GestureArea.NAV_BUTTON, GestureDirection.RIGHT))
    }

    @Test fun `dispatch runs handler except for none`() {
        var received: MutableList<GestureAction> = mutableListOf()
        val handler = GestureHandler { received.add(it) }

        assertTrue(GestureRouter.dispatch(handler, GestureAction.NEW_TAB))
        assertFalse(GestureRouter.dispatch(handler, GestureAction.NONE))
        assertEquals(listOf(GestureAction.NEW_TAB), received)
    }
}
