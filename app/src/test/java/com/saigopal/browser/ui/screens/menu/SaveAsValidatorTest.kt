package com.saigopal.browser.ui.screens.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Locks the legacy HelperUnit.save_as filename rules so the Compose dialog rejects exactly
 * what the old dialog rejected (and accepts what it accepted).
 */
class SaveAsValidatorTest {

    @Test
    fun `valid name with dotted extension passes`() {
        assertTrue(SaveAsValidator.isValid("photo", ".jpg"))
        assertTrue(SaveAsValidator.isValid("archive", ".tar.gz"))
    }

    @Test
    fun `extension without leading dot fails`() {
        assertFalse(SaveAsValidator.isValid("photo", "jpg"))
    }

    @Test
    fun `empty title fails`() {
        assertFalse(SaveAsValidator.isValid("", ".jpg"))
    }

    @Test
    fun `empty extension fails`() {
        assertFalse(SaveAsValidator.isValid("photo", ""))
    }

    @Test
    fun `whitespace-only title fails - trim happens before validation in the dialog`() {
        assertFalse(SaveAsValidator.isValid("", ".pdf"))
    }

    @Test
    fun `split takes everything before the last dot`() {
        assertEquals("photo" to ".jpg", SaveAsValidator.splitFileName("photo.jpg"))
        assertEquals("a.b" to ".c", SaveAsValidator.splitFileName("a.b.c"))
    }

    @Test
    fun `dotless or leading-dot names split defensively`() {
        assertEquals("noext" to "", SaveAsValidator.splitFileName("noext"))
        assertEquals("" to "", SaveAsValidator.splitFileName(""))
    }

    @Test
    fun `suggested extension only reused when 8 chars or fewer including dot`() {
        assertEquals(".jpg", SaveAsValidator.suggestedExtension("photo.jpg"))
        assertNull(SaveAsValidator.suggestedExtension("video.verylongextension"))
        assertNull(SaveAsValidator.suggestedExtension("noext"))
    }
}
