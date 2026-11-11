package org.levimc.launcher.core.crash

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.SystemClock
import org.levimc.launcher.BuildConfig
import org.levimc.launcher.ui.activities.CrashActivity
import org.levimc.launcher.util.LauncherStorage
import xcrash.ICrashCallback
import xcrash.XCrash
import java.io.File

object CrashReporter {
    private const val EXTRA_LOG_PATH = "LOG_PATH"
    private const val EXTRA_SUMMARY = "SUMMARY"
    private const val EXTRA_CRASH_TYPE = "CRASH_TYPE"
    private const val EXTRA_LEGACY_EMERGENCY = "EMERGENCY"
    private const val CRASH_TYPE_JAVA = "JAVA"
    private const val CRASH_TYPE_NATIVE = "NATIVE"
    private const val CRASH_TYPE_ANR = "ANR"

    @Volatile
    private var installed = false

    @Volatile
    private var handlingCrash = false

    @Volatile
    private var uploadProvider: CrashReportUploadProvider = NoCrashReportUploadProvider

    /** Installs the optional upload integration. Existing callers remain compatible. */
    @JvmStatic
    fun installUploadProvider(provider: CrashReportUploadProvider?) {
        uploadProvider = provider ?: NoCrashReportUploadProvider
    }

    @JvmStatic
    fun init(application: Application) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            installed = true

            val appContext = application.applicationContext
            configureUploadProvider(appContext)
            if (isCrashProcess()) return

            val logDir = crashLogDir(appContext)
            XCrash.init(application, XCrash.InitParameters().apply {
                setAppVersion(BuildConfig.VERSION_NAME)
                setLogDir(logDir.absolutePath)
                setJavaCallback(buildCrashCallback(appContext, CRASH_TYPE_JAVA))
                setNativeCallback(buildCrashCallback(appContext, CRASH_TYPE_NATIVE))
                setAnrCallback(buildCrashCallback(appContext, CRASH_TYPE_ANR))
                setJavaRethrow(false)
                setNativeRethrow(false)
                setAnrRethrow(false)
            })
        }
    }

    @JvmStatic
    fun sendUnsentReports() {
        try {
            uploadProvider.sendUnsentReports()
        } catch (_: Throwable) {
            // Optional upload failures must not interrupt launcher or crash recovery.
        }
    }

    @JvmStatic
    fun refreshCrashlyticsCollection(context: Context) {
        configureUploadProvider(context.applicationContext)
    }

    @JvmStatic
    fun isHandlingCrash(): Boolean {
        return handlingCrash
    }

    private fun buildCrashCallback(context: Context, crashType: String): ICrashCallback {
        val appContext = context.applicationContext
        return ICrashCallback { logPath, emergency ->
            handlingCrash = true
            val summary = buildCrashSummary(crashType, emergency)
            try {
                uploadProvider.recordCrash(CrashReportEvent(crashType, logPath, emergency, summary))
            } catch (_: Throwable) {
                // Continue to local recovery even if an upload provider fails.
            }
            sendUnsentReports()
            launchCrashActivity(appContext, crashType, logPath, emergency, summary)
        }
    }

    private fun launchCrashActivity(
        context: Context,
        crashType: String,
        logPath: String?,
        emergency: String?,
        summary: String
    ) {
        try {
            val intent = Intent(context, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_LOG_PATH, logPath)
                putExtra(EXTRA_SUMMARY, summary)
                putExtra(EXTRA_CRASH_TYPE, crashType)
                putExtra(EXTRA_LEGACY_EMERGENCY, emergency)
            }
            context.startActivity(intent)
        } catch (error: Throwable) {
            recordHandlerError(error)
        }
    }

    private fun recordHandlerError(error: Throwable) {
        try {
            uploadProvider.recordException(error)
        } catch (_: Throwable) {
            // Reporting a recovery-handler error must not replace local recovery.
        }
    }

    private fun configureUploadProvider(context: Context) {
        try {
            uploadProvider.configure(context)
        } catch (_: Throwable) {
            // A backend initialization failure must not prevent local crash capture.
        }
    }

    private fun isCrashProcess(): Boolean {
        return Application.getProcessName().endsWith(":crash")
    }

    private fun crashLogDir(context: Context): File {
        val primary = LauncherStorage.getCrashLogsDir(context)
        if (ensureWritableDir(primary)) return primary

        val externalRoot = context.getExternalFilesDir(null)
        if (externalRoot != null) {
            val external = File(externalRoot, "crash_logs")
            if (ensureWritableDir(external)) return external
        }

        val fallback = File(context.filesDir, "crash_logs")
        ensureWritableDir(fallback)
        return fallback
    }

    private fun ensureWritableDir(dir: File): Boolean {
        if (!ensureDir(dir)) return false
        return try {
            val probe = File(dir, ".write_probe_${Process.myPid()}_${SystemClock.uptimeMillis()}")
            if (!probe.createNewFile()) return false
            probe.delete()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun ensureDir(dir: File): Boolean {
        return try {
            if (dir.exists()) dir.isDirectory else dir.mkdirs()
        } catch (_: Throwable) {
            false
        }
    }

    private fun buildCrashSummary(crashType: String, emergency: String?): String {
        val trimmedEmergency = emergency?.lineSequence()
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotBlank() }
        return if (trimmedEmergency.isNullOrBlank()) {
            "xCrash captured $crashType crash"
        } else {
            "xCrash captured $crashType crash: $trimmedEmergency"
        }
    }

}
