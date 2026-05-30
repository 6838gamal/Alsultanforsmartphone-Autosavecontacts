package gamalprojects.autosavecontacts.alsultanformobile.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

enum class LogLevel {
    INFO, WARNING, ERROR, SUCCESS
}

object AppLogger {
    private const val TAG = "AutoSaveCRM"
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    fun log(message: String, tag: String = "SYSTEM", level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry(tag = tag, message = message, level = level)
        val androidLogLevel = when (level) {
            LogLevel.INFO -> Log.INFO
            LogLevel.WARNING -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
            LogLevel.SUCCESS -> Log.INFO
        }
        Log.println(androidLogLevel, "$TAG:$tag", message)

        val currentList = _logsFlow.value.toMutableList()
        currentList.add(0, entry) // Add most recent at the top
        if (currentList.size > 200) {
            currentList.removeAt(currentList.lastIndex)
        }
        _logsFlow.value = currentList
    }

    fun d(message: String, tag: String = "DEBUG") = log(message, tag, LogLevel.INFO)
    fun i(message: String, tag: String = "INFO") = log(message, tag, LogLevel.INFO)
    fun w(message: String, tag: String = "WARN") = log(message, tag, LogLevel.WARNING)
    fun e(message: String, tag: String = "ERROR") = log(message, tag, LogLevel.ERROR)
    fun s(message: String, tag: String = "SUCCESS") = log(message, tag, LogLevel.SUCCESS)

    fun clearLogs() {
        _logsFlow.value = emptyList()
        log("Logs cleared", "SYSTEM", LogLevel.INFO)
    }
}
