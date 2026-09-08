package com.saigopal.browser.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * Golden parity tests: UrlInterpreter must produce byte-identical output to the Java
 * BrowserUnit.isURL/queryWrapper it replaces. Expected values below were derived by
 * executing the ORIGINAL logic rules by hand (regex match → prepend scheme / else encode+prefix).
 */
class UrlInterpreterTest {

    private val ddg = SearchEngine.DUCKDUCKGO

    // ---- isUrl ---------------------------------------------------------------

    @Test fun `bare domain is a url`() {
        assertTrue(UrlInterpreter.isUrl("example.com"))
    }

    @Test fun `uppercase domain is a url`() {
        assertTrue(UrlInterpreter.isUrl("Example.COM"))
    }

    @Test fun `ip address is a url`() {
        assertTrue(UrlInterpreter.isUrl("192.168.1.1"))
    }

    @Test fun `domain with path and query is a url`() {
        assertTrue(UrlInterpreter.isUrl("en.wikipedia.org/wiki/Android?x=1#frag"))
    }

    @Test fun `about blank and mailto and file are urls`() {
        assertTrue(UrlInterpreter.isUrl("about:blank"))
        assertTrue(UrlInterpreter.isUrl("mailto:someone@example.com"))
        assertTrue(UrlInterpreter.isUrl("file:///sdcard/x.html"))
    }

    @Test fun `about startpage is a url`() {
        assertTrue(UrlInterpreter.isUrl("about:startpage"))
    }

    @Test fun `about startpage is case insensitive`() {
        assertTrue(UrlInterpreter.isUrl("ABOUT:StartPage"))
    }

    @Test fun `plain words are not urls`() {
        assertFalse(UrlInterpreter.isUrl("hello world"))
        assertFalse(UrlInterpreter.isUrl("cup & mug"))
    }

    @Test fun `null is not a url`() {
        assertFalse(UrlInterpreter.isUrl(null))
    }

    // ---- interpret: url branch -------------------------------------------------

    @Test fun `bare domain gets https prefix`() {
        assertEquals("https://example.com", UrlInterpreter.interpret("example.com", ddg, ""))
    }

    @Test fun `original casing preserved when prefixing`() {
        assertEquals("https://Example.COM", UrlInterpreter.interpret("Example.COM", ddg, ""))
    }

    @Test fun `full https url unchanged`() {
        assertEquals(
            "https://en.wikipedia.org/wiki/Main_Page",
            UrlInterpreter.interpret("https://en.wikipedia.org/wiki/Main_Page", ddg, ""),
        )
    }

    @Test fun `http scheme not duplicated`() {
        assertEquals("http://neverssl.com", UrlInterpreter.interpret("http://neverssl.com", ddg, ""))
    }

    @Test fun `about and mailto pass through untouched`() {
        assertEquals("about:blank", UrlInterpreter.interpret("about:blank", ddg, ""))
        assertEquals("mailto:a@b.co", UrlInterpreter.interpret("mailto:a@b.co", ddg, ""))
    }

    @Test fun `interpret passes about startpage through untouched`() {
        assertEquals("about:startpage", UrlInterpreter.interpret("about:startpage", ddg, ""))
    }

    // ---- isStartPage -----------------------------------------------------------

    @Test fun `isStartPage detects the scheme and rejects others`() {
        assertTrue(UrlInterpreter.isStartPage("about:startpage"))
        assertTrue(UrlInterpreter.isStartPage("ABOUT:STARTPAGE"))
        assertFalse(UrlInterpreter.isStartPage("about:blank"))
        assertFalse(UrlInterpreter.isStartPage("https://example.com"))
        assertFalse(UrlInterpreter.isStartPage(null))
    }

    // ---- interpret: search branch ------------------------------------------------

    @Test fun `plain query searches duckduckgo with plus encoding`() {
        assertEquals(
            "https://duckduckgo.com/?q=hello+world",
            UrlInterpreter.interpret("hello world", ddg, ""),
        )
    }

    @Test fun `special characters are percent encoded`() {
        assertEquals(
            "https://duckduckgo.com/?q=cup+%26+mug",
            UrlInterpreter.interpret("cup & mug", ddg, ""),
        )
    }

    @Test fun `intent uri falls into search branch like legacy`() {
        assertEquals(
            "https://duckduckgo.com/?q=intent%3A%2F%2Ffoo",
            UrlInterpreter.interpret("intent://foo", ddg, ""),
        )
    }

    @Test fun `google engine prefix used when selected`() {
        assertEquals(
            "https://www.google.com/search?q=android+16",
            UrlInterpreter.interpret("android 16", SearchEngine.GOOGLE, ""),
        )
    }

    @Test fun `custom engine uses stored prefix`() {
        assertEquals(
            "https://kagi.com/search?q=kittens",
            UrlInterpreter.interpret("kittens", SearchEngine.CUSTOM, "https://kagi.com/search?q="),
        )
    }

    @Test fun `blank custom prefix falls back to ddg`() {
        assertEquals(
            "https://duckduckgo.com/?q=x",
            UrlInterpreter.interpret("x", SearchEngine.CUSTOM, ""),
        )
    }

    // ---- google redirect unwrapping ------------------------------------------------

    @Test fun `play store redirect unwrapped`() {
        val wrapped = "www.google.com/url?q=https://play.google.com/store/apps/details?id=x&sa=D"
        assertEquals(
            "https://play.google.com/store/apps/details?id=x",
            UrlInterpreter.interpret(wrapped, ddg, ""),
        )
    }

    // ---- domain helper -------------------------------------------------------------

    @Test fun `domain strips www`() {
        assertEquals("example.com", UrlInterpreter.domainOf("https://www.example.com/path?q=1"))
    }

    @Test fun `domain of garbage is empty`() {
        assertEquals("", UrlInterpreter.domainOf(null))
        assertEquals("", UrlInterpreter.domainOf("not a url at all"))
    }
}
