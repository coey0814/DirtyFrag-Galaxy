# DirtyFrag-Galaxy

<p align="right">
  <a href="README.md">한국어</a> · <b>English</b>
</p>

<p align="center">
  <img src="docs/icon.png" width="180" height="180" alt="DirtyFrag Galaxy icon">
</p>

<p align="center">
  One-click <b>temporary KernelSU root (late-load)</b> for Samsung Galaxy devices<br>
  via the <b>DirtyFrag (CVE-2026-43284)</b> exploit.<br>
  <b>No Shizuku, no PC, no ADB</b> — the app performs everything from the exploit to KernelSU late-load by itself.
</p>

<p align="center">
  <a href="../../releases/latest">Download latest APK</a>
</p>

> ## ⚠️ WARNING
>
> **I am not responsible for bricked phones.**
>
> * This tool uses a temporary (late-load) root and does **not** flash any partition, so it does **not**
>   trip the KNOX warranty bit.
> * It works on DirtyFrag-vulnerable kernels. On a non-vulnerable or incompatible kernel it **stops at the
>   patch/primitive verification step and does not proceed to root acquisition.**
> * **Only run this on a device you own.**
> * While root is active, some apps (banking / government / DRM) may detect root and refuse to run.
> * No warranty; use at your own risk.

---

## Supported devices / kernels

The app selects a kernel module (KO) automatically based on the **Android KMI family (`androidNN`)** and the
kernel **`major.minor`** shown by `uname`.

> `androidNN` is not necessarily the same as the Android version number you see; it means the
> **Android kernel / KMI family** shown by `uname -r`.

Measured results so far:

| Device | Model | Kernel | Result |
| --- | --- | --- | --- |
| Galaxy Z Fold8 Ultra | `SM-F976N` (q8q) | `6.12.58-android16` | ✅ works (baseline) |
| Galaxy S25 | `SM-S931N` (pa1q) | `6.6.98-android15` | ✅ works |
| Galaxy S24+ | `SM-S926N` (e2s) | `6.1.157-android14` | ❌ CBC primitive NO-OP |

* What matters is not a plain `major.minor`, but **whether the kernel primitive DirtyFrag needs actually
  exists and works.**
* So far it worked on `6.6.98-android15` and `6.12.58-android16`; on `6.1.157-android14` the CBC primitive
  was a NO-OP and it did not work.
* **The kernel version number alone does not guarantee vulnerability.** Vendor patches, backports, kernel
  tree differences and the exploit environment can change the result.
* Upstream Linux CVE data has its own fix criteria, so we do not generalize as "6.1 = patched" or
  "6.6 = vulnerable".
* On verified devices, we confirmed **50+ consecutive successful rooting attempts**.

### Bundled KOs

```text
android12-5.10
android13-5.10
android13-5.15
android14-5.15
android14-6.1
android15-6.6
android16-6.12
android17-6.18
```

A KO for each KMI above is bundled. This does not guarantee operation on every target device/firmware.

---

## Requirements

* A **Samsung Galaxy** device whose kernel has the DirtyFrag primitive.
* The **[KernelSU manager](https://github.com/tiann/KernelSU/releases)** app — **not bundled, install separately** (tested v3.3.0).
* (Recommended for modules) **[Zygisk-Next](https://github.com/LSPosed/ZygiskNext)** (tested v1.5.0) ·
  **[LSPosed](https://lsposed.zip)** (tested **LSPosed-it v2.2.0 / build 7854**).
* This app does not flash/modify any partition, so **root is lost on reboot** (enable auto-root to re-apply).

## Usage

1. Install the **[KernelSU manager](https://github.com/tiann/KernelSU/releases)** app (separate).
2. Install this app's APK from [Releases](../../releases/latest).
3. Open the app and tap **루팅 (ROOT)**. (exploit → temporary root → KernelSU late-load)
4. When the root request appears, enable ROOT for this app (DirtyFrag Galaxy) in **KernelSU manager > Superuser**.

### First run (no modules yet) — order matters

> **Modules can only be installed after root is acquired.** Follow the order below.

5. After step 4 the app auto-checks modules and, if missing, shows a
   **"Module installation required" notification** (+ an "Open manager" button).
   In **KernelSU manager → Modules**, install Zygisk-Next and LSPosed (zip).

6. Return to the app — it is **auto-detected** (no need to press ROOT again).
   Once modules are ready, a **"Soft restart required (for LSPosed to work)" notification** appears.

7. Tap **[Soft restart now]** in the notification, or press **소프트 재시작 (SOFT RESTART)** in the app.
   When done, the state becomes **Fully Activated**.

8. Optionally enable **Auto-root on boot** to re-apply root automatically after each reboot.

> Summary: **root → install modules (notification-guided) → auto-detect → soft restart (notification / one tap).**
> Rooting still completes even without modules; instead of a hard failure the app provides notifications,
> guidance, and a one-tap action.

## Highlights

* **No Shizuku / PC / ADB** — no pairing or external tools; the app performs everything from the exploit to KernelSU late-load.
* **100% root success** — on verified devices, **50+ consecutive attempts all succeeded**.
* **Stable soft restart** — once root is granted to this app in the manager, module staging + soft reboot run
  cleanly from a `su` context.
* **7-state live verification + health panel** — exploit / grant / modules / SELinux / Zygisk / LSPosed / restart.
* **Accurate LSPosed detection** — based on the **current `system_server` pid** (`logcat` + lspd verbose,
  mtime-guarded), eliminating false positives/negatives.
* **Multi-KO auto-select** — picks the KO matching the KMI and kernel version.
* **Optional permissive + no-reboot Enforcing restore** — devices that do not need permissive just work;
  on devices that do (6.12) SELinux is switched back to `Enforcing` **without a reboot** after root + Zygisk + LSPosed are confirmed.
* **Auto-root on boot** — foreground service + notifications.
* **First-run module guidance (notification)** — rooting completes even with no modules; the app guides you
  with a **"Module installation required" notification + "Open manager" button**.
* **Auto-detect + soft-restart notification** — returning from the manager **auto-refreshes** detection, and
  once root + modules are ready a **"Soft restart required" notification (one-tap [Soft restart now])** appears.
* **Log tab / aurora visuals** — detailed logs and status snapshots.

---

## Auto-root on boot

Enable **Auto-root on boot**. After a reboot a foreground service runs exploit → `su` → module stage
automatically and shows progress notifications.

**Auto soft restart defaults to OFF** (to avoid loops).
When LSPosed activation is needed, run a soft restart once in the app.

---

## Notes and limitations

* On currently verified devices, **50+ consecutive rooting attempts all succeeded**.
* Results are not guaranteed on unverified devices or other firmware.
* On a **non-vulnerable or incompatible kernel**, it stops with patch/primitive verification messages such as
  `CBC page-cache primitive is a NO-OP ... Aborting.`.
* The stage runs **once only**. `ksud unload` is never used (it hard-locks the device).

---

## Demo videos

<p align="center">
  <video src="https://raw.githubusercontent.com/coey0814/DirtyFrag-Galaxy/main/docs/first_root.mp4"
         width="300" autoplay loop muted playsinline></video>
  <video src="https://raw.githubusercontent.com/coey0814/DirtyFrag-Galaxy/main/docs/soft_restart.mp4"
         width="300" autoplay loop muted playsinline></video>
</p>

<p align="center">
  <sub>Left: first-run rooting · Right: soft restart</sub>
</p>

---

## Screenshots

| Home | Log | Settings |
|:--:|:--:|:--:|
| <img src="docs/home.jpg" width="300" alt="Home screen"> | <img src="docs/log.jpg" width="300" alt="Log screen"> | <img src="docs/settings.jpg" width="300" alt="Settings screen"> |

---

## Build

Requirements: **JDK 17**, **Android SDK 36**, **NDK 27.2.12479018**.

```sh
JAVA_HOME=/path/to/jdk-17 ./gradlew :app:assembleRelease
# output: app/build/outputs/apk/release/dirtyfrag-galaxy.apk
```

`local.properties` is not committed (point `sdk.dir` at your SDK).

The release APK is signed with the **AOSP testkey** committed in this repo
(`keystore/testkey.p12`, alias `testkey`, password `android`), so anyone can rebuild it with the same key.

For personal distribution or a separate release, **replace `keystore/testkey.p12` with your own private
release key and update the signing config accordingly.**

> The public testkey is a default for reproducible builds and is not intended as a signing key for official distribution.

### Bundled asset integrity (SHA-256)

| Asset | SHA-256 (prefix) |
| --- | --- |
| `assets/ksud` | `63046bf5a6409e5f…` |
| `ko/dirtyfrag-android12-5.10.ko` | `8ea7b79bedfba0b5…` |
| `ko/dirtyfrag-android13-5.10.ko` | `97b09bac363ce6bf…` |
| `ko/dirtyfrag-android13-5.15.ko` | `43e5669315b5d710…` |
| `ko/dirtyfrag-android14-5.15.ko` | `f96a0d5ac8f43683…` |
| `ko/dirtyfrag-android14-6.1.ko` | `a05e62319dcac2e7…` |
| `ko/dirtyfrag-android15-6.6.ko` | `6658df7da8b2e90a…` |
| `ko/dirtyfrag-android16-6.12.ko` | `cc4cf69f40d00d6f…` |
| `ko/dirtyfrag-android17-6.18.ko` | `32e30b20a71e2ae8…` |

---

## Credits

This project is based on the following open source and research:

* https://github.com/lsposed/lspromise — original DirtyFrag PoC / exploit chain
* https://github.com/diabl0w/DFRoot — exploit chain / late-load flow
* https://github.com/polygraphene/DFReroot — SELinux permissive KO and related code
* https://github.com/combeng6th/DirtyInit — unprivileged XFRM/IPsec technique
* https://github.com/tiann/KernelSU — KernelSU userspace (`ksud`)
* https://github.com/LSPosed/ZygiskNext — Zygisk-Next
* https://github.com/LSPosed/LSPosed — LSPosed

See [NOTICE](NOTICE) for detailed third-party licenses and source provenance.

---

## License

**Code written directly for this project is distributed under the Apache License 2.0.**
See [LICENSE](LICENSE).

Bundled third-party components remain under their respective original licenses.

* `assets/ksud` — **GNU GPL-3.0-or-later**
* `ko/*.ko` — kernel modules based on the DirtyFrag-related upstream implementation. The actual source
  provenance and license status of each module are described in [NOTICE](NOTICE).
* Where no explicit repository-level license is identified upstream for `ko/*.ko`, we do not arbitrarily
  label them GPL-2.0 or GPL-3.0 and instead **record the original source and its unspecified license status as-is**.

Third-party components are not re-licensed under this project's Apache-2.0 license.
