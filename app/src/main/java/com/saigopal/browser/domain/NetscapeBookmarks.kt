package com.saigopal.browser.domain

/*
 * NetscapeBookmarks (P10 10.3) — byte-compatible port of BrowserUnit's bookmark HTML exchange.
 *
 * Export template is the exact legacy constant, so files round-trip with old releases. Parsing is
 * slightly MORE tolerant than legacy (case-insensitive <dt><a / </a> matching instead of the two
 * hard-coded case combos) while accepting everything the old parser accepted.
 */
object NetscapeBookmarks {

    /** Legacy BrowserUnit.BOOKMARK_TYPE verbatim. */
    private const val LINE_TEMPLATE = "<DT><A HREF=\"{url}\" ADD_DATE=\"{time}\">{title}</A>"

    data class Entry(val title: String, val url: String, val time: Long)

    fun entry(title: String, url: String, time: Long = 0L): Entry = Entry(title, url, time)

    fun exportLine(entry: Entry): String = LINE_TEMPLATE
        .replace("{title}", entry.title)
        .replace("{url}", entry.url)
        .replace("{time}", entry.time.toString())

    /** One line per bookmark, exactly what the legacy exporter wrote (no header/footer). */
    fun buildExport(entries: List<Entry>): String =
        entries.joinToString(separator = "\n") { exportLine(it) }

    /**
     * Legacy import rules: trimmed line must look like `<dt><a …</a>`; URL is the first
     * space-delimited `href="…"`, title is everything after the last `>` of the stripped line.
     * Blank titles/urls are skipped.
     */
    fun parse(text: String): List<Entry> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("<dt><a ", ignoreCase = true) && it.endsWith("</a>", ignoreCase = true) }
            .mapNotNull { line ->
                val title = extractTitle(line)
                val url = extractUrl(line)
                if (title.isBlank() || url.isBlank()) null else Entry(title, url, time = 0L)
            }
            .toList()

    private fun extractTitle(line: String): String {
        val stripped = line.substring(0, line.length - 4) // remove trailing </a>
        val index = stripped.lastIndexOf('>')
        return if (index < 0 || index + 1 > stripped.length) "" else stripped.substring(index + 1)
    }

    private fun extractUrl(line: String): String {
        for (token in line.split(Regex(" +"))) {
            // Legacy did substring(6, len-1) unguarded, which threw on a bare href=" token and
            // aborted the whole import via its catch-all; we skip the malformed token instead.
            if (token.startsWith("href=\"", ignoreCase = true) && token.length > 6) {
                return token.substring(6, token.length - 1) // strip href=" and closing "
            }
        }
        return ""
    }
}
