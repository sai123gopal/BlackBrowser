package com.saigopal.browser.ui.screens.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhitelistValidationTest {

    @Test
    fun `ordinary domains pass`() {
        assertTrue(isValidDomainEntry("example.com"))
        assertTrue(isValidDomainEntry("sub.example.co.uk"))
        assertTrue(isValidDomainEntry("m-site.example.org"))
    }

    @Test
    fun `blank input fails`() {
        assertFalse(isValidDomainEntry(""))
    }

    @Test
    fun `whitespace anywhere fails`() {
        assertFalse(isValidDomainEntry("example .com"))
        assertFalse(isValidDomainEntry(" example.com"))
    }

    @Test
    fun `url with scheme fails - user should enter bare domain`() {
        assertFalse(isValidDomainEntry("https://example.com"))
    }

    @Test
    fun `over-253-characters fails`() {
        val longDomain = "a".repeat(250) + ".com"
        assertFalse(isValidDomainEntry(longDomain))
    }

    @Test
    fun `exactly-253-character domain passes`() {
        val exact = "a".repeat(249) + ".com"
        org.junit.Assert.assertEquals(253, exact.length)
        assertTrue(isValidDomainEntry(exact))
    }

    @Test
    fun `path or query characters fail`() {
        assertFalse(isValidDomainEntry("example.com/path"))
        assertFalse(isValidDomainEntry("example.com?q=1"))
    }

    @Test
    fun `underscore survives - legacy whitelists contained them`() {
        assertTrue(isValidDomainEntry("under_score.example.com"))
    }
}
