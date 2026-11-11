package org.levimc.launcher.integration.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.levimc.launcher.BuildConfig
import org.levimc.launcher.core.crash.CrashReportEvent
import org.levimc.launcher.core.crash.CrashReportUploadProvider
import org.levimc.launcher.settings.FeatureSettings

/** Crashlytics adapter. Failures here never prevent local crash logging or launcher startup. */
class FirebaseCrashReportUploadProvider : CrashReportUploadProvider {
    private val maxValueLength = 1024

    override fun configure(context: Context) {
        if (!ensureFirebaseInitialized(context.applicationContext)) return
        try {
            FirebaseCrashlytics.getInstance().apply {
                val uploadEnabled = isUploadEnabled()
                setCrashlyticsCollectionEnabled(uploadEnabled)
                if (!uploadEnabled) {
                    deleteUnsentReports()
                    return
                }
                setBuildMetadata()
            }
        } catch (_: Throwable) {
            // Crash upload is optional and must not affect local launcher behavior.
        }
    }

    override fun sendUnsentReports() {
        if (!isAvailable() || !isUploadEnabled()) return
        try {
            FirebaseCrashlytics.getInstance().sendUnsentReports()
        } catch (_: Throwable) {
            // Firebase can be absent or temporarily unavailable.
        }
    }

    override fun recordCrash(event: CrashReportEvent) {
        if (!isAvailable() || !isUploadEnabled()) return
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("last_crash_type", event.crashType)
                setCustomKey("last_crash_summary", trim(event.summary))
                setBuildMetadata()
                if (!event.logPath.isNullOrBlank()) {
                    setCustomKey("local_crash_log", trim(event.logPath))
                    log("xCrash local log: ${event.logPath}")
                }
                if (!event.emergency.isNullOrBlank()) {
                    log("xCrash emergency: ${trim(event.emergency)}")
                }
                recordException(XCrashCapturedException(event.crashType, event.summary))
            }
        } catch (_: Throwable) {
            // The local xCrash log remains available if upload fails.
        }
    }

    override fun recordException(error: Throwable) {
        if (!isAvailable() || !isUploadEnabled()) return
        try {
            FirebaseCrashlytics.getInstance().recordException(error)
        } catch (_: Throwable) {
            // Ignore errors from the optional reporting backend.
        }
    }

    private fun ensureFirebaseInitialized(context: Context): Boolean {
        if (!isAvailable()) return false
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context)
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }

    private fun isAvailable(): Boolean = BuildConfig.BUILD_TYPE in setOf("debug", "release", "beta")

    private fun isUploadEnabled(): Boolean = try {
        FeatureSettings.getInstance().isCrashUploadEnabled
    } catch (_: Throwable) {
        true
    }

    private fun FirebaseCrashlytics.setBuildMetadata() {
        setCustomKey("version_name", BuildConfig.VERSION_NAME)
        setCustomKey("version_code", BuildConfig.VERSION_CODE)
        setCustomKey("is_beta", BuildConfig.IS_BETA)
        setCustomKey("build_type", BuildConfig.BUILD_TYPE)
    }

    private fun trim(value: String): String =
        if (value.length > maxValueLength) value.take(maxValueLength) else value

    private class XCrashCapturedException(crashType: String, summary: String) :
        RuntimeException("xCrash captured $crashType crash: $summary")
}
