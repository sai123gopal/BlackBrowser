package com.saigopal.browser.data.repo

import android.app.Application
import com.saigopal.browser.browser.AdBlock
import com.saigopal.browser.browser.Cookie
import com.saigopal.browser.browser.Javascript
import com.saigopal.browser.browser.Remote
import com.saigopal.browser.unit.RecordUnit

/*
 * WhitelistRepository — one API over the four near-identical legacy classes
 * (AdBlock / Javascript / Cookie / Remote). Deletes ~600 lines of copy-paste when the
 * whitelist UI moves to Compose in P03; the legacy classes stay as the storage adapters.
 */

enum class WhitelistKind(val table: String, val label: String) {
    AD_BLOCK(RecordUnit.TABLE_WHITELIST, "Ad blocker"),
    JAVASCRIPT(RecordUnit.TABLE_JAVASCRIPT, "JavaScript"),
    COOKIE(RecordUnit.TABLE_COOKIE, "Cookies"),
    REMOTE_CONTENT(RecordUnit.TABLE_REMOTE, "Remote content"),
}

/**
 * The four legacy whitelist classes (AdBlock/Javascript/Cookie/Remote) expose identical APIs
 * but share no supertype, so a `when` over them infers `Any`. This interface gives the
 * repository a common type; each legacy class implements it directly.
 */
interface WhitelistAdapter {
    fun isWhite(url: String?): Boolean
    fun addDomain(domain: String)
    fun removeDomain(domain: String)
    fun clearDomains()
}

class WhitelistRepository(private val app: Application) {

    private fun adapter(kind: WhitelistKind): WhitelistAdapter = when (kind) {
        WhitelistKind.AD_BLOCK -> AdBlock(app)
        WhitelistKind.JAVASCRIPT -> Javascript(app)
        WhitelistKind.COOKIE -> Cookie(app)
        WhitelistKind.REMOTE_CONTENT -> Remote(app)
    }

    /** Is [url]'s host exempted for [kind]? (Legacy contains-match semantics preserved.) */
    fun isWhitelisted(kind: WhitelistKind, url: String?): Boolean = adapter(kind).isWhite(url)

    fun addDomain(kind: WhitelistKind, domain: String) {
        adapter(kind).addDomain(domain.trim())
    }

    fun removeDomain(kind: WhitelistKind, domain: String) {
        adapter(kind).removeDomain(domain.trim())
    }

    fun clearDomains(kind: WhitelistKind) {
        adapter(kind).clearDomains()
    }

    /** Full list for the management screen; reads straight from the DB on IO. */
    suspend fun listDomains(kind: WhitelistKind): List<String> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val action = com.saigopal.browser.database.RecordAction(app.applicationContext)
            try {
                action.open(false)
                action.listDomains(kind.table)
            } finally {
                runCatching { action.close() }
            }
        }
}
