package dirtyfrag.galaxy.core

import android.content.Context

/** Health / module detection. Detection only — this app never downloads or installs. */
class Verifier(private val ctx: Context) {

    fun device(): DeviceInfo = SysUtil.deviceInfo()

    /** True when the KernelSU-family su userspace is granted to this app (uid 0). */
    fun suGranted(): Boolean {
        // A cold boot installs su asynchronously; retry briefly to avoid a transient
        // false "권한 없음" in the UI right after rooting.
        repeat(3) { i ->
            if (SysUtil.isRootGranted()) return true
            if (i < 2) runCatching { Thread.sleep(1500) }
        }
        return false
    }

    /** Ephemeral root present (exploit hook active). */
    fun rootPresent(): Boolean = java.io.File("/dev/df").exists()

    fun isInstalled(pkg: String): Boolean = try {
        ctx.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: Throwable) {
        false
    }

    /** Root checker that distinguishes the exploit hook from a granted su. */
    fun root(): HealthStatus {
        val devDf = rootPresent()
        val su = suGranted()
        val ok = devDf || su
        val detail = when {
            devDf && su -> "/dev/df + su uid=0"
            devDf -> "/dev/df present (su 미허용)"
            su -> "su uid=0"
            else -> "no root"
        }
        return HealthStatus(Health.ROOT, ok, detail)
    }

    /**
     * SELinux state — informational, NOT a success gate.
     *
     * The app (unprivileged) usually cannot read /sys/fs/selinux/enforce, which made
     * the old code report "unknown"; and treating permissive as a hard requirement
     * broke devices that finish rooting while Enforcing (e.g. SM-S931N / 6.6.98).
     * Now we surface the real state and mark the row ok as long as it is detectable.
     */
    fun selinux(): HealthStatus {
        // `su` may return an error string (e.g. "Cannot run program ...") instead of a
        // state; only accept an actual state word so we never print errors as "ok".
        val r = SysUtil.su("getenforce")
        val ge = if (r.code == 0)
            r.out.lineSequence().map { it.trim() }
                .firstOrNull { it.equals("Enforcing", true) || it.equals("Permissive", true) }.orEmpty()
        else ""
        val enf = SysUtil.readFile("/sys/fs/selinux/enforce")?.trim()
        val enforcing = ge.equals("Enforcing", true) || enf == "1"
        val permissive = ge.equals("Permissive", true) || enf == "0"
        val detail = when {
            ge.isNotEmpty() -> ge
            permissive -> "Permissive"
            enforcing -> "Enforcing"
            else -> "미확인"
        }
        // ok == "state known"; the row is never treated as a failure.
        return HealthStatus(Health.PERMISSIVE, enforcing || permissive, detail)
    }

    /** True when SELinux is currently Enforcing (best-effort). */
    fun isEnforcing(): Boolean {
        val r = SysUtil.su("getenforce")
        if (r.code == 0) {
            val line = r.out.lineSequence().map { it.trim() }
                .firstOrNull { it.equals("Enforcing", true) || it.equals("Permissive", true) }
            if (line != null) return line.equals("Enforcing", true)
        }
        return SysUtil.readFile("/sys/fs/selinux/enforce")?.trim() == "1"
    }

    @Deprecated("use selinux()", ReplaceWith("selinux()"))
    fun permissive(): HealthStatus = selinux()

    fun zygisk(): HealthStatus {
        if (!suGranted()) return HealthStatus(Health.ZYGISK, false, "su 미허용")
        val ps = SysUtil.su("ps -A -o NAME 2>/dev/null | grep -E 'zn-|zygiskd|rezygisk|neozygisk' | head -1").out.trim()
        if (ps.isNotEmpty()) return HealthStatus(Health.ZYGISK, true, "injected")
        val mod = SysUtil.su("ls /data/adb/modules 2>/dev/null | grep -Ei 'zygisk|rezygisk|neozygisk' | grep -vi lsposed | head -1").out.trim()
        return HealthStatus(Health.ZYGISK, mod.isNotEmpty(), if (mod.isEmpty()) "not injected" else "module present")
    }

    /**
     * True when LSPosed has actually initialized inside system_server.
     *
     * The old check (grep "lsposed" in /proc/<system_server>/maps) was a FALSE POSITIVE:
     * the manager APK is mapped into system_server even when the framework is NOT injected
     * (value is 2 in both states). With Zygisk-Next's anonymous-memory mode the module .so
     * is hidden from maps entirely.
     *
     * Reliable signal: the framework logs under tag "LSPosedFramework" with the process
     * label "(system)" when running inside system_server. It is absent right after a
     * cold boot + exploit and appears only after a soft reboot.
     */
    private fun systemServerHooked(): Boolean {
        // Detect whether the CURRENT system_server process has the LSPosed framework loaded.
        //
        // Rejected signals:
        //  - grep "lsposed" in /proc/<ss>/maps  -> false positive (manager APK is mapped anyway)
        //  - raw /data/adb/lspd/log/verbose_*.log -> false positive across boots (stale file)
        //  - logcat alone -> false negative (the single "(system)" line rotates out)
        //
        // Reliable: match the CURRENT system_server pid.
        //  * live:    logcat "LSPosedFramework" lines carry the pid in the pid column.
        //  * durable: the lspd daemon's verbose log for THIS boot holds lines tagged with
        //             that pid (format "uid: pid: tid ..."), guarded by file mtime >= boot time.
        repeat(4) { i ->
            val pid = SysUtil.su("pidof system_server 2>/dev/null").out.trim()
                .split(Regex("\\s+")).firstOrNull().orEmpty()
            if (pid.isNotEmpty()) {
                val l = SysUtil.su("logcat -d -s LSPosedFramework 2>/dev/null | grep -c \" $pid \"").out.trim().toIntOrNull() ?: 0
                if (l > 0) return true

                val up = SysUtil.su("cut -d. -f1 /proc/uptime").out.trim().toLongOrNull() ?: 0L
                val bootEpoch = System.currentTimeMillis() / 1000 - up
                // Under enforcing / restricted /data, this may fail with a Permission-denied
                // message on stdout; require an actual absolute path before using it.
                val v = SysUtil.su("ls -t /data/adb/lspd/log/verbose_* 2>/dev/null | head -1").out.trim()
                if (v.startsWith("/")) {
                    val vm = SysUtil.su("stat -c %Y \"$v\" 2>/dev/null").out.trim().toLongOrNull() ?: 0L
                    if (vm >= bootEpoch - 5) {
                        val g = SysUtil.su("grep -c \" $pid:\" \"$v\" 2>/dev/null").out.trim().toIntOrNull() ?: 0
                        if (g > 0) return true
                    }
                }
            }
            if (i < 3) runCatching { Thread.sleep(1200) }
        }
        return false
    }

    fun lsposed(): HealthStatus = lsposed(systemServerHooked())

    private fun lsposed(hooked: Boolean): HealthStatus {
        val manager = isInstalled(LSPOSED_MANAGER_PKG)
        if (!suGranted()) {
            return HealthStatus(Health.LSPOSED, false, if (manager) "manager만 설치 (미활성)" else "미설치")
        }
        val lspd = SysUtil.su("ps -A -o NAME 2>/dev/null | grep -E 'lspd|lsposed' | head -1").out.trim()
        if (lspd.isEmpty()) return HealthStatus(Health.LSPOSED, false, "lspd 미기동")
        return if (hooked) HealthStatus(Health.LSPOSED, true, "system_server 주입됨")
        else HealthStatus(Health.LSPOSED, false, "system_server 미주입")
    }

    fun hook(): HealthStatus = hook(systemServerHooked())

    private fun hook(hooked: Boolean): HealthStatus {
        val ok = suGranted() && hooked
        return HealthStatus(Health.HOOK, ok, if (ok) "framework active" else "미주입")
    }

    fun healthAll(): List<HealthStatus> {
        // Evaluate the system_server hook ONCE so every row is consistent.
        val hooked = systemServerHooked()
        return listOf(root(), selinux(), zygisk(), lsposed(hooked), hook(hooked))
    }

    fun installedModules(): Set<String> {
        if (!suGranted()) return emptySet()
        val out = SysUtil.su("ls /data/adb/modules 2>/dev/null").out
        return out.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /** Detection of the 3 recommended modules (manager / LSPosed / Zygisk-Next). */
    fun moduleStatuses(): List<ModuleStatus> {
        val mods = installedModules()
        val su = suGranted()
        val manager = isInstalled(KERNELSU_PKG)
        // Require the actual module (e.g. "zygisk_lsposed"), not just the manager app,
        // so a first-time user is correctly told to install the LSPosed module too.
        val lsposedMod = mods.any { it.contains("lsposed", true) }
        val lsposed = lsposedMod
        // Zygisk provider modules are named "zygisksu" (Zygisk-Next), "rezygisk",
        // "neozygisk", … — but NOT the LSPosed module "zygisk_lsposed".
        val zygisk = mods.any { it.contains("zygisk", true) && !it.contains("lsposed", true) }
        val noSu = if (su) "미설치" else "su 필요"
        return listOf(
            ModuleStatus(ModuleKind.MANAGER, if (manager) ModuleState.INSTALLED else ModuleState.NOT_INSTALLED, if (manager) KERNELSU_PKG else "미설치"),
            ModuleStatus(
                ModuleKind.LSPOSED,
                if (lsposed) ModuleState.INSTALLED else ModuleState.NOT_INSTALLED,
                when {
                    lsposed -> "module present"
                    isInstalled(LSPOSED_MANAGER_PKG) -> "모듈 설치 필요"
                    else -> noSu
                }
            ),
            ModuleStatus(ModuleKind.ZYGISK, if (zygisk) ModuleState.INSTALLED else ModuleState.NOT_INSTALLED, if (zygisk) "module present" else noSu)
        )
    }

    fun modulesOk(): Boolean = moduleStatuses().all { it.state == ModuleState.INSTALLED }
}
