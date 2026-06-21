package online.faph.netmon

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Talks to the EXISTING NetMon backend (pwa.php) — same endpoint/payload the PWA uses.
 * No server changes required. The native app is just a more accurate pusher (real bytes).
 */
object Api {
    const val ENDPOINT = "https://netmon.faph.online/api/pwa.php"
    const val API_KEY = "fa_netmon_9vaXe0_2026_xK7mPqR3"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun post(params: Map<String, String>): JSONObject {
        val body = params.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        return try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText) ?: "{}"
            try { JSONObject(text) } catch (e: Exception) {
                JSONObject().put("status", "error").put("raw", text)
            }
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("raw", e.message ?: "network")
        } finally {
            conn.disconnect()
        }
    }

    /** One-time registration. Returns user_id on success, else null. */
    fun register(c: Context, nickname: String): String? {
        val res = post(
            mapOf(
                "action" to "register",
                "key" to API_KEY,
                "nickname" to nickname,
                "device_id" to Prefs.deviceId(c),
                "device_name" to Net.deviceModel(),
                "device_type" to "Phone",
                "platform" to "Android",
                "screen" to "native"
            )
        )
        if (res.optString("status") == "ok") {
            val uid = res.optString("user_id", "")
            val nick = res.optString("nickname", nickname)
            if (uid.isNotEmpty()) {
                Prefs.setUserId(c, uid)
                Prefs.setNickname(c, nick)
                return uid
            }
        }
        return null
    }

    /**
     * Push one DELTA sample (bytes since last successful push). Baseline only advances on
     * success, so a failed push never loses data. Returns true on ok.
     */
    fun push(c: Context): Boolean {
        val uid = Prefs.getUserId(c) ?: return false
        val nick = Prefs.getNickname(c) ?: return false

        val cur = Net.totalBytes()
        val last = Prefs.getLastBytes(c)
        val delta = when {
            cur < 0 -> 0L          // metering unsupported
            last < 0 -> 0L          // first sample: establish baseline, report 0
            cur < last -> cur       // counter reset (reboot) -> treat current as delta
            else -> cur - last
        }

        val now = System.currentTimeMillis()
        val lastTs = Prefs.getLastPushTs(c)
        val mins = if (lastTs > 0) ((now - lastTs) / 60000L).toInt().coerceAtLeast(0) else 0

        val res = post(
            mapOf(
                "action" to "push",
                "key" to API_KEY,
                "user_id" to uid,
                "nickname" to nick,
                "device_id" to Prefs.deviceId(c),
                "session_minutes" to mins.toString(),
                "data_bytes" to delta.toString(),
                "cat_browsing" to "0",
                "cat_streaming" to "0",
                "cat_gaming" to "0",
                "cat_social" to "0",
                "cat_other" to delta.toString(),
                "current_activity" to "auto",
                "battery_pct" to Net.batteryPct(c).toString(),
                "battery_charging" to Net.batteryCharging(c).toString(),
                "connection" to "WIFI",
                "online" to "1",
                "consent" to "yes"
            )
        )
        val ok = res.optString("status") == "ok"
        if (ok) {
            if (cur >= 0) Prefs.setLastBytes(c, cur)   // advance baseline ONLY on success
            Prefs.setLastPushTs(c, now)
        }
        return ok
    }
}
