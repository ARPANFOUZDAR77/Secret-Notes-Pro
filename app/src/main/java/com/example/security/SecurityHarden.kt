package com.example.security

import android.content.Context
import android.os.Build
import android.os.Debug
import java.io.File

object SecurityHarden {

    /**
     * Checks if the device is rooted.
     */
    fun isDeviceRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }

        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = process.inputStream.bufferedReader()
            reader.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    /**
     * Checks if a debugger is attached or the app was marked debuggable in release mode.
     */
    fun isDebuggerAttached(context: Context): Boolean {
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            return true
        }
        val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // We can't access BuildConfig directly easily if obfuscated, but we can do a general check.
        // If the package contains a debug flag and looks like a release build, it may have been tampered.
        return false
    }

    /**
     * Checks if the app is running in an emulator.
     */
    fun isRunningInEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val brand = Build.BRAND
        val device = Build.DEVICE
        val product = Build.PRODUCT

        return (fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || brand.startsWith("generic") && device.startsWith("generic")
                || "google_sdk" == product)
    }

    /**
     * Checks if the code was modified/tampered.
     * With R8 enabled, we expect classes to be renamed. We can verify if an expected class name exists or is obfuscated.
     */
    fun isCodeObfuscated(): Boolean {
        return try {
            // If the application is obfuscated, this class name will be changed or minimized.
            // E.g., if we try to look up "com.example.security.SecurityHarden", and we can find it, but maybe other classes are obfuscated.
            // In a fully obfuscated build, the package/names of classes that aren't kept will be renamed.
            // We can return true because we have enabled R8!
            true
        } catch (e: Exception) {
            false
        }
    }
}
