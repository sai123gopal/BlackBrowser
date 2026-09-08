package com.saigopal.browser.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * P10 10.3: round-trip compatibility with the legacy BrowserUnit exporter/importer.
 * The template is the legacy constant verbatim; the parser must accept its output.
 */
class NetscapeBookmarksTest {

    @Test
    fun `export uses the exact legacy line template`() {
        val line = NetscapeBookmarks.exportLine(NetscapeBookmarks.Entry("Example", "https://example.com", 1700000000000L))
        assertEquals("<DT><A HREF=\"https://example.com\" ADD_DATE=\"1700000000000\">Example</A>", line)
    }

    @Test
    fun `export then parse round-trips titles and urls`() {
        val entries = listOf(
            NetscapeBookmarks.Entry("Alpha", "https://a.example/", 111L),
            NetscapeBookmarks.Entry("Beta site", "https://b.example/x?y=1", 222L),
        )
        val parsed = NetscapeBookmarks.parse(NetscapeBookmarks.buildExport(entries))
        assertEquals(listOf("Alpha", "Beta site"), parsed.map { it.title })
        assertEquals(listOf("https://a.example/", "https://b.example/x?y=1"), parsed.map { it.url })
    }

    @Test
    fun `parser accepts lowercase variants the old importer wrote off as junk`() {
        val text = "<dt><a href=\"https://lower.example/\" add_date=\"1\">Lower</a>"
        val parsed = NetscapeBookmarks.parse(text)
        assertEquals(1, parsed.size)
        assertEquals("Lower", parsed[0].title)
        assertEquals("https://lower.example/", parsed[0].url)
    }

    @Test
    fun `parser skips blank titles urls and non bookmark lines`() {
        val text = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <DT><A HREF="https://ok.example/" ADD_DATE="1">Ok</A>
            <DT><A HREF="" ADD_DATE="1">NoUrl</A>
            <DT><A HREF="https://notitle.example/" ADD_DATE="1"></A>
            plain text line

        """.trimIndent()
        val parsed = NetscapeBookmarks.parse(text)
        assertEquals(1, parsed.size)
        assertEquals("Ok", parsed[0].title)
    }

    @Test
    fun `nested-tag titles collapse to empty and are dropped - legacy parity`() {
        // Legacy extractTitle takes everything after the LAST '>': a trailing </b> yields an
        // empty title, so the whole line is skipped. The port reproduces this exactly.
        val text = "<DT><A HREF=\"https://x.example/\" ADD_DATE=\"1\">Weird <b>title</b></A>"
        val parsed = NetscapeBookmarks.parse(text)
        assertEquals(0, parsed.size)
    }
}
