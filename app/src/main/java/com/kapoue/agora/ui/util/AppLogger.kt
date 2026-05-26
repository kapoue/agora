package com.kapoue.agora.ui.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogger @Inject constructor() {

    private data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String
    )

    private val entries = ArrayDeque<LogEntry>()
    private val maxSize = 2000

    fun d(tag: String, msg: String) = add("D", tag, msg)
    fun i(tag: String, msg: String) = add("I", tag, msg)
    fun w(tag: String, msg: String) = add("W", tag, msg)
    fun e(tag: String, msg: String) = add("E", tag, msg)

    @Synchronized
    private fun add(level: String, tag: String, msg: String) {
        if (entries.size >= maxSize) entries.removeFirst()
        entries.addLast(LogEntry(System.currentTimeMillis(), level, tag, msg))
        val priority = when (level) { "E" -> Log.ERROR; "W" -> Log.WARN; "I" -> Log.INFO; else -> Log.DEBUG }
        Log.println(priority, tag, msg)
    }

    @Synchronized
    fun export(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return buildString {
            appendLine("=== Agora Logs — ${sdf.format(System.currentTimeMillis())} ===")
            appendLine("${entries.size} entrées")
            appendLine()
            entries.forEach { e ->
                appendLine("${sdf.format(e.timestamp)} [${e.level}] ${e.tag}: ${e.message}")
            }
        }
    }

    @Synchronized
    fun clear() = entries.clear()
}
