package com.example.sampleview.crash

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Debug
import androidx.core.content.ContextCompat
import com.example.sampleview.AppActivityManager
import com.example.sampleview.AppLogger
import com.example.sampleview.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object JavaCrashHandler : Thread.UncaughtExceptionHandler {
    private val lock = ReentrantLock()
    private var logDir: File? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appInfo: String = "App info not found\n"
    private var locationInfo: String = "Location info not found\n"
    private val crashTimeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
    private val crashFileFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
    private val locationTimeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }

    fun init(context: Context) {
        initAppInfo(context)
        initLocationInfo(context)
        logDir = File(context.filesDir, "crash_logs")
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        runCatching {
            Thread.setDefaultUncaughtExceptionHandler(this)
        }.onFailure {
            AppLogger.e(tag = "JavaCrashHandler", msg = "Failed to set default uncaught exception handler", throwable = it)
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (defaultHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
        }
        runCatching {
            handleException(thread, throwable)
        }.onFailure {
            AppLogger.e(tag = "JavaCrashHandler", "Error while handling uncaught exception", throwable = it)
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun handleException(thread: Thread, throwable: Throwable) {
        val now = Date()
        val fileName = "crash_${crashFileFormat.get()?.format(now)}.log"
        val separator = "\n" + "-".repeat(128) + "\n\n"
        val crashLog = buildString {
            append(buildCrashInfo(thread, throwable))
            append(separator)
        }
        runCatching {
            lock.withLock {
                logDir?.apply {
                    if (!exists()) mkdirs()
                    val file = resolve(fileName)
                    BufferedWriter(FileWriter(file, true)).use { writer ->
                        writer.append(crashLog)
                        writer.flush()
                    }
                }
            }
        }.onFailure {
            Timber.e(it, "Failed to write crash log")
        }
    }

    private fun buildCrashInfo(thread: Thread, throwable: Throwable): String {
        return buildString {
            appendLine("Crash Time: ${crashTimeFormat.get()?.format(Date())}")
            appendLine()
            appendLine("App in foreground: ${AppActivityManager.isAppInForeground}")
            appendLine("CurrentActivity: ${AppActivityManager.currentActivityName}")
            appendLine()
            appendLine("Thread Info:")
            appendLine("  Name: ${thread.name}")
            appendLine("  ID: ${thread.id}")
            appendLine("  Priority: ${thread.priority}")
            appendLine("  State: ${thread.state}")
            appendLine()
            appendLine("Device Info:")
            appendLine("  Brand: ${Build.BRAND}")
            appendLine("  Device: ${Build.DEVICE}")
            appendLine("  Model: ${Build.MODEL}")
            appendLine("  Id: ${Build.ID}")
            appendLine("  Product: ${Build.PRODUCT}")
            appendLine("  SDK: ${Build.VERSION.SDK_INT}")
            appendLine("  Release: ${Build.VERSION.RELEASE}")
            appendLine("  Incremental: ${Build.VERSION.INCREMENTAL}")
            appendLine()
            appendLine("App Info:")
            appendLine(appInfo)
            appendLine(locationInfo)
            appendLine("Memory Info:")
            val runtime = Runtime.getRuntime()
            appendLine("  Total Memory: ${runtime.totalMemory() / 1024} KB")
            appendLine("  Free Memory: ${runtime.freeMemory() / 1024} KB")
            appendLine("  Max Memory: ${runtime.maxMemory() / 1024} KB")
            appendLine("  Native Heap Size: ${Debug.getNativeHeapSize() / 1024} KB")
            appendLine("  Native Heap Allocated: ${Debug.getNativeHeapAllocatedSize() / 1024} KB")
            appendLine("  Native Heap Free: ${Debug.getNativeHeapFreeSize() / 1024} KB")
            appendLine()
            appendLine("Stacktrace:")
            appendLine(getFullStackTrace(throwable))
        }
    }

    private fun initAppInfo(context: Context) {
        appInfo = runCatching {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            buildString {
                appendLine("  VersionName: ${info.versionName}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    appendLine("  VersionCode: ${info.longVersionCode}")
                } else {
                    @Suppress("DEPRECATION") appendLine("  VersionCode: ${info.versionCode}")
                }
                appendLine("  PackageName: ${info.packageName}")
            }
        }.getOrElse {
            "  App info not found\n"
        }
    }

    private fun getFullStackTrace(t: Throwable): String {
        val sb = StringBuilder()
        var current: Throwable? = t
        while (current != null) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            current.printStackTrace(pw)
            sb.append(sw.toString())
            current = current.cause
            if (current != null) sb.append("\nCaused by:\n")
        }
        return sb.toString()
    }

    private fun initLocationInfo(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationInfo = "Location: permission not granted\n"
            return
        }
        locationInfo = runCatching {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val providers = manager.getProviders(true)
            val locInfo = providers.asReversed().firstNotNullOfOrNull { provider ->
                manager.getLastKnownLocation(provider)?.let { location ->
                    buildString {
                        appendLine("Location Info:")
                        appendLine("  Provider: $provider")
                        appendLine("  Latitude: ${location.latitude}")
                        appendLine("  Longitude: ${location.longitude}")
                        appendLine("  Accuracy: ${location.accuracy}m")
                        appendLine("  Time: ${locationTimeFormat.get()?.format(Date(location.time))}")
                    }
                }
            }
            locInfo ?: "Location: last known location not available\n"
        }.getOrElse {
            "Location error: ${it.message}\n"
        }
    }
}
