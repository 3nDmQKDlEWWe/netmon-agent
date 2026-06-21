package online.faph.netmon

import android.content.Context
import java.util.UUID

/** Simple SharedPreferences wrapper — the native app's own persistent identity + counters. */
object Prefs {
    private const val FILE = "netmon"
    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Stable device id generated once on this device (no fingerprint guessing needed). */
    fun deviceId(c: Context): String {
        val s = sp(c)
        var id = s.getString("device_id", null)
        if (id == null) {
            id = "and-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24)
            s.edit().putString("device_id", id).apply()
        }
        return id
    }

    fun getNickname(c: Context): String? = sp(c).getString("nickname", null)
    fun setNickname(c: Context, v: String) = sp(c).edit().putString("nickname", v).apply()

    fun getUserId(c: Context): String? = sp(c).getString("user_id", null)
    fun setUserId(c: Context, v: String) = sp(c).edit().putString("user_id", v).apply()

    /** Last cumulative byte counter (TrafficStats since boot). -1 = no baseline yet. */
    fun getLastBytes(c: Context): Long = sp(c).getLong("last_bytes", -1L)
    fun setLastBytes(c: Context, v: Long) = sp(c).edit().putLong("last_bytes", v).apply()

    fun getLastPushTs(c: Context): Long = sp(c).getLong("last_push_ts", 0L)
    fun setLastPushTs(c: Context, v: Long) = sp(c).edit().putLong("last_push_ts", v).apply()

    fun isRegistered(c: Context): Boolean = getUserId(c) != null && getNickname(c) != null
}
