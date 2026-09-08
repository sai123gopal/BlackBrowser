package com.saigopal.browser.ui.screens.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Ports of the legacy FeedbackActivity validation rules, kept verbatim so existing
 * console-side feedback tooling keeps receiving well-formed nodes.
 */
class FeedbackValidatorTest {

    // --- email regex: ^[\w.-]+@([\w\-]+\.)+[A-Z]{2,4}$ (case-insensitive) ---

    @Test
    fun `plain addresses are valid`() {
        assertTrue(FeedbackValidator.isEmailValid("user@example.com"))
        assertTrue(FeedbackValidator.isEmailValid("first.last@sub.domain.org"))
    }

    @Test
    fun `short and long tlds are valid`() {
        assertTrue(FeedbackValidator.isEmailValid("a@b.de"))
        assertTrue(FeedbackValidator.isEmailValid("a@b.info"))
    }

    @Test
    fun `missing tld is invalid`() {
        assertFalse(FeedbackValidator.isEmailValid("user@example"))
    }

    @Test
    fun `five-letter tld is invalid`() {
        assertFalse(FeedbackValidator.isEmailValid("user@example.museum"))
    }

    @Test
    fun `missing at sign is invalid`() {
        assertFalse(FeedbackValidator.isEmailValid("user.example.com"))
    }

    @Test
    fun `empty string is invalid`() {
        assertFalse(FeedbackValidator.isEmailValid(""))
    }

    // --- feedback body: trimmed length must exceed 15 characters ---

    @Test
    fun `fifteen characters is too short`() {
        assertFalse(FeedbackValidator.isFeedbackValid("123456789012345"))
    }

    @Test
    fun `sixteen characters passes`() {
        assertTrue(FeedbackValidator.isFeedbackValid("1234567890123456"))
    }

    @Test
    fun `surrounding whitespace does not count toward length`() {
        assertFalse(FeedbackValidator.isFeedbackValid("   123456789012   "))
        assertTrue(FeedbackValidator.isFeedbackValid("    1234567890123456    "))
    }

    @Test
    fun `blank input is rejected`() {
        assertFalse(FeedbackValidator.isFeedbackValid(""))
        assertFalse(FeedbackValidator.isFeedbackValid("              "))
    }
}
