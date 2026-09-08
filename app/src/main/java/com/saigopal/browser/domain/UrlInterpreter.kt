package com.saigopal.browser.domain

import java.net.URLEncoder

/*
 * UrlInterpreter — Kotlin port of BrowserUnit.isURL + BrowserUnit.queryWrapper.
 *
 * The regex and the decision order are copied VERBATIM from the Java original so existing
 * users' typed input behaves identically. Golden tests in UrlInterpreterTest lock the parity.
 * Pure JVM (no android.* imports) so it tests without an emulator.
 */

object UrlInterpreter {

    // ---- verbatim from BrowserUnit -----------------------------------------
    private const val URL_ABOUT_BLANK = "about:blank"
    private const val URL_SCHEME_ABOUT = "about:"

    /** Native start page scheme (P07) — not in legacy BrowserUnit, added for the Compose shell. */
    const val URL_START_PAGE = "about:startpage"

    private const val URL_SCHEME_MAIL_TO = "mailto:"
    private const val URL_SCHEME_FILE = "file://"
    private const val URL_SCHEME_HTTP = "https://"

    private const val URL_PREFIX_GOOGLE_PLAY = "www.google.com/url?q="
    private const val URL_SUFFIX_GOOGLE_PLAY = "&sa"
    private const val URL_PREFIX_GOOGLE_PLUS = "plus.url.google.com/url?q="
    private const val URL_SUFFIX_GOOGLE_PLUS = "&rct"

    private val urlPattern = PatternHolder.PATTERN

    private object PatternHolder {
        val PATTERN = Regex(
            "^((ftp|http|https|intent)?://)"
                + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?"
                + "(([0-9]{1,3}\\.){3}[0-9]{1,3}"
                + "|"
                + "(.)*"
                + "([0-9a-z_!~*'()-]+\\.)*"
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\."
                + "[a-z]{2,6})"
                + "(:[0-9]{1,4})?"
                + "((/?)|"
                + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$",
            RegexOption.IGNORE_CASE,
        )
    }
    // -------------------------------------------------------------------------

    /** Parity port of `BrowserUnit.isURL`. */
    fun isUrl(url: String?): Boolean {
        if (url == null) return false
        val lowered = url.lowercase()
        if (lowered.startsWith(URL_ABOUT_BLANK) ||
            lowered == URL_START_PAGE ||
            lowered.startsWith(URL_SCHEME_MAIL_TO) ||
            lowered.startsWith(URL_SCHEME_FILE)
        ) {
            return true
        }
        return urlPattern.matches(lowered)
    }

    /** True when [url] targets the native start page rendered by the Compose shell. */
    fun isStartPage(url: String?): Boolean = url != null && url.lowercase() == URL_START_PAGE

    /**
     * Turn omnibox input into a loadable URL: unwraps Google Play/Plus redirects, keeps
     * scheme-less URLs, otherwise searches with [engine] (or [customPrefix] when engine==CUSTOM).
     */
    fun interpret(query: String, engine: SearchEngine, customPrefix: String): String {
        var q = query
        val temp = query.lowercase()

        if (temp.contains(URL_PREFIX_GOOGLE_PLAY) && temp.contains(URL_SUFFIX_GOOGLE_PLAY)) {
            val start = temp.indexOf(URL_PREFIX_GOOGLE_PLAY) + URL_PREFIX_GOOGLE_PLAY.length
            val end = temp.indexOf(URL_SUFFIX_GOOGLE_PLAY)
            q = query.substring(start, end)
        } else if (temp.contains(URL_PREFIX_GOOGLE_PLUS) && temp.contains(URL_SUFFIX_GOOGLE_PLUS)) {
            val start = temp.indexOf(URL_PREFIX_GOOGLE_PLUS) + URL_PREFIX_GOOGLE_PLUS.length
            val end = temp.indexOf(URL_SUFFIX_GOOGLE_PLUS)
            q = query.substring(start, end)
        }

        if (isUrl(q)) {
            if (q.startsWith(URL_SCHEME_ABOUT) || q.startsWith(URL_SCHEME_MAIL_TO)) return q
            if (!q.contains("://")) q = URL_SCHEME_HTTP + q
            return q
        }

        val encoded = try {
            URLEncoder.encode(q, Charsets.UTF_8.name())
        } catch (_: Exception) {
            q
        }
        return when (engine) {
            SearchEngine.GOOGLE -> "https://www.google.com/search?q="
            SearchEngine.BING -> "http://www.bing.com/search?q="
            SearchEngine.BAIDU -> "https://www.baidu.com/s?wd="
            SearchEngine.SEARX -> "https://searx.be/?q="
            SearchEngine.QWANT -> "https://www.qwant.com/?q="
            SearchEngine.CUSTOM -> customPrefix.ifBlank { "https://duckduckgo.com/?q=" }
            SearchEngine.DUCKDUCKGO -> "https://duckduckgo.com/?q="
        } + encoded
    }

    /** Host of [url] with `www.` stripped; empty string on failure. Port of HelperUnit.domain (JVM-safe). */
    fun domainOf(url: String?): String {
        if (url == null) return ""
        return try {
            val host = java.net.URI(url).host ?: fallbackHost(url) ?: return ""
            host.removePrefix("www.").trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun fallbackHost(url: String): String? {
        // java.net.URI rejects some hosts WebView accepts (underscores etc.) — best effort split.
        val rest = url.substringAfter("://", url)
        return rest.substringBefore('/').substringBefore(':').takeIf { it.isNotEmpty() }
    }

    /** True for internal pages that must never hit the network or history. */
    fun isInternalScheme(url: String?): Boolean =
        url != null && (
            url.startsWith(URL_SCHEME_ABOUT) ||
                url.startsWith(URL_SCHEME_MAIL_TO) ||
                url.startsWith("intent://")
            )
}
