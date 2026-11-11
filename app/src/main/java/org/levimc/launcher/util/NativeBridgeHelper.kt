package org.levimc.launcher.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.levimc.launcher.core.minecraft.LauncherApplication
import androidx.core.content.edit
import android.widget.Toast
import android.view.Gravity
import org.levimc.launcher.R
import androidx.annotation.Keep

@Keep
object NativeBridgeHelper {
    private const val TAG = "NativeBridgeHelper"

    @Volatile
    private var gxcoreLoaded = false

    @Volatile
    private var gxcoreLoadFailure: String? = null

    @Volatile
    private var gxcoreLoadCause: Throwable? = null

    @JvmStatic
    fun getAppContext(): Context {
        return LauncherApplication.context
    }

    @JvmStatic
    fun ensureGxCoreLoaded(): Boolean {
        if (gxcoreLoaded) {
            return true
        }
        return synchronized(this) {
            if (gxcoreLoaded) {
                true
            } else {
                try {
                    System.loadLibrary("gxcore")
                    gxcoreLoaded = true
                    gxcoreLoadFailure = null
                    gxcoreLoadCause = null
                    true
                } catch (error: LinkageError) {
                    recordGxCoreFailure("System.loadLibrary(gxcore)", error)
                    false
                } catch (error: SecurityException) {
                    recordGxCoreFailure("System.loadLibrary(gxcore)", error)
                    false
                }
            }
        }
    }

    @JvmStatic
    fun getGxCoreLoadFailure(): String? = gxcoreLoadFailure

    @JvmStatic
    fun getGxCoreLoadCause(): Throwable? = gxcoreLoadCause

    private fun recordGxCoreFailure(operation: String, error: Throwable) {
        val detail = NativeLibraryDiagnostic.describeFailure(error)
        gxcoreLoadFailure = detail
        gxcoreLoadCause = error
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        Log.e(TAG, "$operation failed for arm64 launcher native bridge (device ABI: $abi): $detail", error)
    }

    @JvmStatic
    fun scanImage(inputPath: String): Boolean {
        if (!ensureGxCoreLoaded()) {
            return false
        }
        return nativeScanImage(inputPath)
    }

    @JvmStatic
    fun rewriteImage(inputPath: String, outputPath: String): Boolean {
        if (!ensureGxCoreLoaded()) {
            return false
        }
        return nativeRewriteImage(inputPath, outputPath)
    }

    @JvmStatic
    fun bootstrapGxCore(): Boolean {
        if (!ensureGxCoreLoaded()) {
            return false
        }
        return try {
            nativeBootstrapGxCore()
            true
        } catch (error: LinkageError) {
            recordGxCoreFailure("gxcore bootstrap", error)
            false
        } catch (error: RuntimeException) {
            recordGxCoreFailure("gxcore bootstrap", error)
            false
        }
    }

    @JvmStatic
    fun loadMinecraftRuntime(libraryPath: String) {
        if (!ensureGxCoreLoaded()) {
            throw UnsatisfiedLinkError("Failed to load gxcore for the Minecraft runtime")
        }
        nativeLoadMinecraftRuntime(libraryPath)?.let { error ->
            throw UnsatisfiedLinkError(error)
        }
    }

    @JvmStatic
    fun getPrefs(name: String): SharedPreferences {
        return getAppContext().getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getBoolean(prefsName: String, key: String, defaultValue: Boolean = false): Boolean {
        return try {
            getPrefs(prefsName).getBoolean(key, defaultValue)
        } catch (t: Throwable) {
            defaultValue
        }
    }

    @JvmStatic
    fun putBoolean(prefsName: String, key: String, value: Boolean) {
        try {
            getPrefs(prefsName).edit { putBoolean(key, value) }
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun getString(prefsName: String, key: String, defaultValue: String? = null): String? {
        return try {
            getPrefs(prefsName).getString(key, defaultValue)
        } catch (t: Throwable) {
            defaultValue
        }
    }

    @JvmStatic
    fun putString(prefsName: String, key: String, value: String) {
        try {
            getPrefs(prefsName).edit { putString(key, value) }
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun isLauncherManagedLoginEnabled(): Boolean {
        return try {
            val json = getString("feature_settings", "settings_json", null)
            if (!json.isNullOrEmpty()) {
                val obj = JSONObject(json)
                obj.optBoolean("launcherManagedMcLoginEnabled", false)
            } else {
                false
            }
        } catch (t: Throwable) {
            false
        }
    }

    @JvmStatic
    fun showInvalidLicenseOverlay() {
        val ctx = getAppContext()
        try {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.invalid_license_detected_toast),
                    Toast.LENGTH_LONG
                ).apply {
                    setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 100)
                    show()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    @JvmStatic
    private external fun nativeScanImage(inputPath: String): Boolean

    @JvmStatic
    private external fun nativeRewriteImage(inputPath: String, outputPath: String): Boolean

    @JvmStatic
    private external fun nativeBootstrapGxCore()

    @JvmStatic
    private external fun nativeLoadMinecraftRuntime(libraryPath: String): String?
}
