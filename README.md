# NetMon Agent (Native Android)

Tunay na per-device internet metering para sa **WiFiCo / NetMon**. Native Kotlin app na
nagme-measure ng **totoong bytes** (hindi estimate) gamit ang `TrafficStats`, tapos
nagpu-push kada ~15 min sa **existing** `pwa.php` backend — **walang server changes**.

- Package: `online.faph.netmon`
- Endpoint na ginagamit: `https://netmon.faph.online/api/pwa.php` (action=register, action=push)
- minSdk 24 · targetSdk 34 · Kotlin 1.9.24 · AGP 8.5.2 · Gradle 8.7

> **Walang kailangang Android Studio.** Nagse-self-build ang GitHub Actions — nag-iinstall
> ito ng sariling Gradle 8.7, kaya HINDI na kailangan ng `gradlew`/wrapper sa repo.

---

## Build sa GitHub Actions (cloud — walang Android Studio)

### 1. Push sa GitHub
```bash
cd netmon-agent
git init
git add .
git commit -m "NetMon native agent v1"
git branch -M main
git remote add origin https://github.com/<USERNAME_MO>/netmon-agent.git
git push -u origin main
```

### 2. Kunin ang APK
GitHub → repo mo → tab na **Actions** → buksan ang latest na "Build APK" run →
i-download ang artifact na **`netmon-agent-debug`** (`app-debug.apk`).
(Pwede ring i-trigger manually: Actions → Build APK → **Run workflow**.)

> Walang Git? Pwede ring i-upload ang mga file via GitHub web:
> repo → **Add file › Upload files** → i-drag lahat ng laman ng `netmon-agent/`
> (kasama ang `.github/` folder) → Commit. Awtomatikong tatakbo ang Actions.

---

## Install sa phone (sideload)
1. Ilipat ang `app-debug.apk` sa phone (USB/Drive/chat).
2. Settings → payagan ang **Install unknown apps** para sa File Manager/Browser.
3. I-tap ang APK → Install → buksan ang **NetMon Agent**.
4. I-type ang nickname → **Sumali sa NetMon** → tapos **Push ngayon (test)**.

---

## Verify (sa server, Bitvise/SSH)
```bash
# Dapat may laman na ang device_id, at tama ang nickname:
/usr/bin/mariadb -u thefxplt_netmon -p'9vaXe0OQH6W^' thefxplt_netmon \
  -e "SELECT id,nickname,device_id,last_seen FROM netmon_users ORDER BY last_seen DESC LIMIT 12;"

# Tingnan ang papasok na bytes per push (delta, hindi lumolobo):
/usr/bin/mariadb -u thefxplt_netmon -p'9vaXe0OQH6W^' thefxplt_netmon \
  -e "SELECT user_id,data_bytes,pushed_at FROM netmon_activity ORDER BY pushed_at DESC LIMIT 15;"
```

---

## Paano gumagana (technical)
- **Metering:** `TrafficStats.getTotalRxBytes()+getTotalTxBytes()` (cumulative since boot,
  zero permission). Ang `data_bytes` na ipinapadala ay **delta** mula sa huling matagumpay
  na push; umuusad lang ang baseline kapag OK ang push (walang data loss kung failed).
- **Reboot-safe:** kapag bumaba ang counter (reboot), ang current na halaga ang itinuturing
  na delta.
- **Background:** `WorkManager` periodic (15-min minimum na pinapayagan ng Android).
- **Identity:** stable `device_id` (`and-...`) na nakaimbak sa app — mas matatag kaysa
  browser fingerprint, kaya kusang mapupunan ang `netmon_users.device_id` (COALESCE sa server).

## Mga limitasyon + susunod na upgrade
- `TrafficStats` total = WiFi + mobile data. Para **WiFi-only + per-app**, ang upgrade ay
  `NetworkStatsManager` (kailangan ng one-time **Usage Access** permission). Naka-design para
  madaling palitan ang `Net.kt` para dito.
- 15-min ang minimum ng WorkManager. Para **5-min** (katugma ng PWA), kailangan ng
  foreground service na may persistent notification (next iteration).
- iOS: hindi pinapayagan ng Apple ang device-wide metering kahit native — PWA estimate /
  router ang gamit doon.

## Build lokal (optional, kung may Gradle 8.7 + JDK 17 ka)
```bash
cd netmon-agent
gradle assembleDebug          # APK -> app/build/outputs/apk/debug/app-debug.apk
```
