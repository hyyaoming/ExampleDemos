package com.example.sampleview.log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * 异步日志管理器，支持日志文件轮转、压缩、异步写入和定时刷新。
 *
 * @param logDir 日志文件存储目录
 * @param maxFileSize 单个日志文件最大大小，超过后触发轮转，默认3MB
 * @param maxFileCount 日志文件最大数量，轮转时旧文件被删除或覆盖，默认10个
 * @param flushCount 写入日志多少条后主动刷新文件，默认5条
 * @param flushIntervalMs 定时刷新的间隔毫秒数，默认1500毫秒
 * @param compressLogs 是否压缩历史日志文件，默认启用
 * @param externalScope 外部协程作用域，默认为null，内部自动创建
 */
class AsyncLogManager(
    private val logDir: File,
    private val maxFileSize: Long = 3 * 1024 * 1024,
    private val maxFileCount: Int = 10,
    private val flushCount: Int = 5,
    private val flushIntervalMs: Long = 1500L,
    private val compressLogs: Boolean = true,
    private val externalScope: CoroutineScope? = null,
) {
    // 协程作用域，外部提供则使用外部，否则内部创建一个SupervisorJob + IO调度的Scope
    private val scope = externalScope ?: CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 当前日志文件索引
    private var currentFileIndex = 0

    // 当前写入的缓冲字符流
    private var bufferedWriter: BufferedWriter? = null

    // 写入锁，避免并发写入冲突
    private val writeLock = Mutex()

    // 轮转锁，避免轮转过程中的竞态
    private val rotateLock = Mutex()

    // 文件流相关操作锁，保护打开关闭文件流及压缩操作
    private val fileMutex = Mutex()

    // 统计未flush的写入条数
    private val unflushedCount = AtomicInteger(0)

    // 定时刷新任务，避免重复启动
    private var flushJob: Job? = null

    // 写入失败计数，用于简单重试策略
    private val writeFailCount = AtomicInteger(0)

    // 初始化状态标志，避免重复初始化
    private val initialized = AtomicBoolean(false)

    /**
     * 初始化，必须调用一次，确保日志目录存在并打开当前日志文件写入流
     */
    suspend fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            if (!logDir.exists()) {
                if (!logDir.mkdirs()) throw IllegalStateException("Failed to create log directory: ${logDir.absolutePath}")
            }
            fileMutex.withLock {
                openCurrentFile()
            }
        }
    }

    /** 获取指定索引的日志文件（压缩后缀根据参数决定） */
    private fun getLogFile(index: Int, compressed: Boolean): File =
        File(logDir, "log_$index.txt${if (compressed) ".gz" else ""}")

    /** 获取指定索引的未压缩临时日志文件，用于写入 */
    private fun getTempLogFile(index: Int): File =
        File(logDir, "log_$index.txt")

    /** 打开当前日志文件的写入流（追加模式） */
    private suspend fun openCurrentFile() {
        closeCurrentStreams() // 关闭旧流，避免资源泄漏
        val file = getTempLogFile(currentFileIndex)
        bufferedWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true), Charsets.UTF_8))
    }

    /** 关闭当前写入流 */
    private suspend fun closeCurrentStreams() {
        try {
            bufferedWriter?.flush()
            bufferedWriter?.close()
        } catch (e: Exception) {
            println("closeCurrentStreams error: ${e.message}")
        } finally {
            bufferedWriter = null
        }
    }

    /**
     * 写入日志条目，包含文件大小检测、文件轮转、写入重试及刷新调度
     */
    suspend fun writeLog(entry: LogEntry) {
        check(initialized.get()) { "AsyncLogManager not initialized, please call initialize() first" }

        writeLock.withLock {
            // 确保写入流打开
            fileMutex.withLock {
                if (bufferedWriter == null) openCurrentFile()
            }

            val logLine = entry.toLogLine() + "\n"
            val bytesSize = logLine.toByteArray(Charsets.UTF_8).size

            // 写入前检测文件大小，超过限制先轮转文件
            fileMutex.withLock {
                val tempFile = getTempLogFile(currentFileIndex)
                val currentSize = tempFile.length()
                if (currentSize + bytesSize >= maxFileSize) {
                    rotateLock.withLock {
                        rotateLog()
                    }
                }
            }

            // 写入内容，重试3次
            var success = false
            var attempt = 0
            while (attempt < 3 && !success) {
                try {
                    fileMutex.withLock {
                        bufferedWriter?.write(logLine)
                    }
                    unflushedCount.incrementAndGet()
                    writeFailCount.set(0)
                    success = true
                } catch (e: Exception) {
                    attempt++
                    writeFailCount.incrementAndGet()
                    println("writeLog error attempt $attempt: ${e.message}")
                    delay(100)
                }
            }
            if (!success) {
                println("writeLog failed after retries: $logLine")
                // 这里可以扩展报警或备用存储策略
            }

            // 写入后再次检测文件大小，确保轮转不遗漏
            fileMutex.withLock {
                val tempFile = getTempLogFile(currentFileIndex)
                if (tempFile.length() >= maxFileSize) {
                    rotateLock.withLock {
                        rotateLog()
                    }
                }
            }

            // 根据未flush条数判断是否立即刷新，否则启动定时刷新
            if (unflushedCount.get() >= flushCount) flush()
            else scheduleFlush()
        }
    }

    /**
     * 轮转日志文件流程：
     * 关闭当前写入流 -> 压缩当前日志文件 -> 删除最旧日志文件 -> 更新索引 -> 打开新日志文件
     */
    private suspend fun rotateLog() {
        fileMutex.withLock {
            closeCurrentStreams()

            // 压缩当前文件（如果启用）
            if (compressLogs) {
                try {
                    compressFile(currentFileIndex)
                } catch (e: Exception) {
                    println("compressFile error: ${e.message}")
                }
            }

            // 删除最旧日志并将其他文件名后移
            try {
                deleteOldestAndShiftFiles()
            } catch (e: Exception) {
                println("deleteOldestAndShiftFiles error: ${e.message}")
            }

            // 更新索引，循环使用文件槽
            currentFileIndex = (currentFileIndex + 1) % maxFileCount
            openCurrentFile()
        }
    }

    /**
     * 压缩指定索引的临时日志文件为 gzip 格式，并删除原文件
     */
    private suspend fun compressFile(index: Int) = withContext(Dispatchers.IO) {
        val sourceFile = getTempLogFile(index)
        if (!sourceFile.exists() || sourceFile.length() == 0L) return@withContext

        val gzipFile = getLogFile(index, compressed = true)
        FileInputStream(sourceFile).use { fis ->
            GZIPOutputStream(FileOutputStream(gzipFile)).use { gos ->
                fis.copyTo(gos)
            }
        }
        sourceFile.delete()
    }

    /**
     * 删除最旧日志文件，并将其他日志文件索引顺延
     */
    private suspend fun deleteOldestAndShiftFiles() = withContext(Dispatchers.IO) {
        val oldestIndex = maxFileCount - 1

        // 删除最旧压缩文件
        val oldestFile = getLogFile(oldestIndex, compressed = true)
        if (oldestFile.exists()) oldestFile.delete()

        // 文件名顺序后移
        for (i in oldestIndex downTo 1) {
            val src = getLogFile(i - 1, compressed = true)
            val dst = getLogFile(i, compressed = true)
            if (src.exists()) {
                if (!src.renameTo(dst)) {
                    println("Failed to rename log file from ${src.name} to ${dst.name}")
                }
            }
        }
    }

    /**
     * 定时刷新调度，避免频繁flush
     */
    private fun scheduleFlush() {
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            try {
                delay(flushIntervalMs)
                flush()
            } finally {
                flushJob = null
            }
        }
    }

    /**
     * 立即刷新缓冲区，写入磁盘
     */
    suspend fun flush() {
        val jobToCancel = flushJob
        fileMutex.withLock {
            try {
                bufferedWriter?.flush()
                unflushedCount.set(0)
            } catch (e: Exception) {
                println("flush error: ${e.message}")
            }
        }
        // 放锁外取消任务，防止死锁
        jobToCancel?.cancel()
    }

    /**
     * 读取最新日志条目，倒序返回，最多 limit 条
     */
    suspend fun getLogs(limit: Int): List<LogEntry> = withContext(Dispatchers.IO) {
        check(initialized.get()) { "AsyncLogManager not initialized, please call initialize() first" }
        val logs = mutableListOf<LogEntry>()

        fileMutex.withLock {
            for (offset in 0 until maxFileCount) {
                if (logs.size >= limit) break
                val index = (currentFileIndex - 1 - offset + maxFileCount) % maxFileCount
                val file = getLogFile(index, compressed = true)
                if (!file.exists()) continue

                try {
                    GZIPInputStream(FileInputStream(file)).bufferedReader(Charsets.UTF_8).useLines { lines ->
                        lines.mapNotNull { LogEntry.fromLogLine(it) }
                            .forEach {
                                if (logs.size < limit) logs.add(it)
                            }
                    }
                } catch (e: Exception) {
                    println("getLogs error reading file ${file.name}: ${e.message}")
                }
            }
        }

        // 按时间倒序返回
        logs.sortedByDescending { it.timestamp }
    }

    /**
     * 关闭日志管理器，停止刷新协程，关闭文件流
     */
    suspend fun close() {
        flushJob?.cancelAndJoin()
        fileMutex.withLock {
            closeCurrentStreams()
        }
        scope.cancel()
    }
}
