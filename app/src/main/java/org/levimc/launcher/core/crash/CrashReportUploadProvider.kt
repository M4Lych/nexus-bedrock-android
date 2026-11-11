package org.levimc.launcher.core.crash

import android.content.Context

/** Optional destination for crash reports. Local crash capture must not depend on this provider. */
interface CrashReportUploadProvider {
    fun configure(context: Context)
    fun sendUnsentReports()
    fun recordCrash(event: CrashReportEvent)
    fun recordException(error: Throwable)
}

/** Data passed to optional crash upload integrations. */
data class CrashReportEvent(
    val crashType: String,
    val logPath: String?,
    val emergency: String?,
    val summary: String
)

/** Used when no upload integration is installed or available. */
object NoCrashReportUploadProvider : CrashReportUploadProvider {
    override fun configure(context: Context) = Unit
    override fun sendUnsentReports() = Unit
    override fun recordCrash(event: CrashReportEvent) = Unit
    override fun recordException(error: Throwable) = Unit
}
