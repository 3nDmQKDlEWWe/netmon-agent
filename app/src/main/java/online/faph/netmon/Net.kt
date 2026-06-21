package online.faph.netmon

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build

/**
 * REAL metering via TrafficStats — total device bytes (rx+tx) since boot, all interfaces.
 * Zero special permission needed. (Future upgrade: NetworkStatsManager for WiFi-only +
 * per-app, which needs the one-time "Usage Access" permission.)
 */
object Net {

    /** Total device bytes (rx+tx) since boot. -1 if unsupported. */
    fun totalBytes(): Long {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val unsupported = TrafficStats.UNSUPPORTED.toLong()
        if (rx == unsupported || tx == unsupported) return -1L
        return rx + tx
    }

    fun batteryPct(c: Context): Int {
        val i = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return -1
        return level * 100 / scale
    }

    fun batteryCharging(c: Context): Int {
        val i = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = st == BatteryManager.BATTERY_STATUS_CHARGING ||
                st == BatteryManager.BATTERY_STATUS_FULL
        return if (charging) 1 else 0
    }

    fun deviceModel(): String {
        val model = Build.MODEL ?: "Android"
        val maker = Build.MANUFACTURER ?: ""
        val name = if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model"
        return name.trim().take(40)
    }
}
