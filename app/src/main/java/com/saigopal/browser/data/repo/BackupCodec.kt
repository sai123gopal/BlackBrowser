package com.saigopal.browser.data.repo

import com.saigopal.browser.domain.BBRecord
import org.json.JSONArray
import org.json.JSONObject

/*
 * BackupCodec (P10 10.3) — versioned full-profile backup format.
 *
 * v2 layout:
 * {
 *   "format": 2,
 *   "prefs":   { key: string|bool|int|long|double, ... },        // SharedPreferences snapshot
 *   "bookmarks": [ {"t","u","c","time"}, ... ],
 *   "tiles":     [ {"t","u","time"}, ... ],
 *   "whitelists": { "AD_BLOCK": [...], "JAVASCRIPT": [...], "COOKIE": [...], "REMOTE_CONTENT": [...] },
 *   "sessions":  [ {"t","u"}, ... ]                               // TabManager session rows
 * }
 */
object BackupCodec {

    const val FORMAT_VERSION = 2

    data class Profile(
        val prefs: Map<String, Any?> = emptyMap(),
        val bookmarks: List<BBRecord> = emptyList(),
        val tiles: List<BBRecord> = emptyList(),
        val whitelists: Map<WhitelistKind, List<String>> = emptyMap(),
        /** Title/url rows in TabManager's session-store shape. */
        val sessions: List<Pair<String, String>> = emptyList(),
    )

    fun encode(profile: Profile): String {
        val root = JSONObject()
        root.put("format", FORMAT_VERSION)

        val prefsJson = JSONObject()
        for ((key, value) in profile.prefs) {
            when (value) {
                null -> Unit // org.json has no null primitive; drop absent values
                is String -> prefsJson.put(key, value)
                is Boolean -> prefsJson.put(key, value)
                is Int -> prefsJson.put(key, value)
                is Long -> prefsJson.put(key, value)
                is Float -> prefsJson.put(key, value.toDouble())
                is Double -> prefsJson.put(key, value)
                // StringSet survives as a JSON array of strings (SharedPreferences' only set type).
                is Set<*> -> prefsJson.put(key, JSONArray(value.map { it.toString() }))
                else -> prefsJson.put(key, value.toString())
            }
        }
        root.put("prefs", prefsJson)

        root.put("bookmarks", recordsToJson(profile.bookmarks, withColor = true))
        root.put("tiles", recordsToJson(profile.tiles, withColor = false))

        val wl = JSONObject()
        profile.whitelists.forEach { (kind, list) ->
            if (list.isNotEmpty()) {
                wl.put(kind.name, JSONArray(list))
            }
        }
        root.put("whitelists", wl)

        val sessions = JSONArray()
        profile.sessions.forEach { (title, url) ->
            sessions.put(JSONObject().put("t", title).put("u", url))
        }
        root.put("sessions", sessions)
        return root.toString()
    }

    fun decode(text: String): Profile {
        val root = JSONObject(text)
        require(root.optInt("format") == FORMAT_VERSION) { "Unsupported backup format ${root.optInt("format")}" }

        val prefs = mutableMapOf<String, Any?>()
        val prefsJson = root.optJSONObject("prefs") ?: JSONObject()
        prefsJson.keys().forEach { key ->
            when (val value = prefsJson.get(key)) {
                // The only collection SharedPreferences stores is StringSet.
                is JSONArray -> prefs[key] =
                    (0 until value.length()).map { value.getString(it) }.toSet()
                is java.math.BigDecimal -> {
                    if (value.scale() <= 0) {
                        val longVal = value.toLong()
                        if (longVal in Int.MIN_VALUE..Int.MAX_VALUE) {
                            prefs[key] = longVal.toInt()
                        } else {
                            prefs[key] = longVal
                        }
                    } else {
                        prefs[key] = value.toDouble()
                    }
                }
                is java.math.BigInteger -> {
                    val longVal = value.toLong()
                    if (longVal in Int.MIN_VALUE..Int.MAX_VALUE) {
                        prefs[key] = longVal.toInt()
                    } else {
                        prefs[key] = longVal
                    }
                }
                else -> prefs[key] = value
            }
        }

        return Profile(
            prefs = prefs,
            bookmarks = jsonToRecords(root.optJSONArray("bookmarks")),
            tiles = jsonToRecords(root.optJSONArray("tiles")),
            whitelists = decodeWhitelists(root.optJSONObject("whitelists")),
            sessions = buildList {
                val arr = root.optJSONArray("sessions") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(o.optString("t") to o.optString("u"))
                }
            },
        )
    }

    private fun decodeWhitelists(obj: JSONObject?): Map<WhitelistKind, List<String>> {
        if (obj == null) return emptyMap()
        return buildMap {
            WhitelistKind.entries.forEach { kind ->
                val arr = obj.optJSONArray(kind.name) ?: return@forEach
                val list = (0 until arr.length()).map { arr.getString(it) }
                if (list.isNotEmpty()) {
                    put(kind, list)
                }
            }
        }
    }

    private fun recordsToJson(records: List<BBRecord>, withColor: Boolean): JSONArray {
        val arr = JSONArray()
        records.forEach { r ->
            val o = JSONObject()
                .put("t", r.title)
                .put("u", r.url)
                .put("time", r.time)
            if (withColor) o.put("c", r.colorCode)
            arr.put(o)
        }
        return arr
    }

    private fun jsonToRecords(arr: JSONArray?): List<BBRecord> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            BBRecord(
                title = o.optString("t"),
                url = o.optString("u"),
                time = o.optLong("time"),
                colorCode = o.optInt("c"),
            )
        }
    }
}
