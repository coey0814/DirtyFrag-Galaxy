package dirtyfrag.galaxy.core

import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/** Thin wrappers around shell execution (app context and clean su context). */
object SysUtil {

    data class Result(val code: Int, val out: String, val timedOut: Boolean = false) {
        val ok get() = code == 0 && !timedOut
    }

    /** Run as the app's shell user. */
    fun exec(command: String, timeoutMs: Long = 15_000): Result = run(arrayOf("/system/bin/sh", "-c", command), timeoutMs)

    /** Run through the KernelSU su placeholder. Fails fast (ENOENT) when not granted. */
    fun su(command: String, timeoutMs: Long = 30_000): Result = run(arrayOf("/system/bin/su", "-c", command), timeoutMs)

    private fun run(argv: Array<String>, timeoutMs: Long): Result {
        return try {
            val p = ProcessBuilder(*argv).redirectErrorStream(true).start()
            val sb = StringBuilder()
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val readerThread = Thread {
                try {
                    var line = reader.readLine()
                    while (line != null) {
                        sb.append(line).append('\n')
                        line = reader.readLine()
                    }
                } catch (_: Exception) {
                }
            }
            readerThread.start()
            val finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                p.destroyForcibly()
                return Result(-1, sb.toString(), timedOut = true)
            }
            readerThread.join(1000)
            Result(p.exitValue(), sb.toString())
        } catch (t: Throwable) {
            Result(-1, t.message ?: t.toString())
        }
    }

    fun prop(name: String): String = try {
        val c = Class.forName("android.os.SystemProperties")
        val m = c.getMethod("get", String::class.java)
        (m.invoke(null, name) as? String).orEmpty()
    } catch (_: Throwable) {
        ""
    }

    fun isRootGranted(): Boolean {
        val r = su("id -u")
        if (r.code != 0) return false
        return r.out.lineSequence().firstOrNull()?.trim() == "0"
    }

    fun readFile(path: String): String? = try {
        val f = java.io.File(path)
        if (f.exists()) f.readText() else null
    } catch (_: Throwable) {
        null
    }

    fun deviceInfo(): DeviceInfo = DeviceInfo(
        model = Build.MODEL ?: "",
        firmware = Build.DISPLAY ?: Build.ID ?: "",
        androidRelease = Build.VERSION.RELEASE ?: "",
        kernel = prop("ro.kernel.version").ifEmpty { System.getProperty("os.version").orEmpty() }
    )
}
