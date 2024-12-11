package com.amazonaws.sfc.log

import com.amazonaws.sfc.system.isWindowsSystem
import java.text.SimpleDateFormat
import java.util.*

class ConsoleLogWriter() : LogWriter {

    var noColor : Boolean = false

    override fun write(logLevel: LogLevel, timestamp: Long, source: String?, message: String) {
        val output = if (logLevel == LogLevel.ERROR) System.err else System.out
        val dtm = "%-23s".format(SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SS").format(Date()))
        val levelStr = coloredLevelString(logLevel)
        val sourceStr = if (source != null && logLevel == LogLevel.TRACE) "[$source] : " else ""
        output.println("$dtm $levelStr-$sourceStr $message")
    }

    override fun close() {
    }

    private fun coloredLevelString(level: LogLevel): String {
        val s = "%-6s".format(level)
        return if (noColor || isWindowsSystem()) s else
            when (level) {
                LogLevel.TRACE -> BLUE
                LogLevel.ERROR -> RED
                LogLevel.INFO -> GREEN
                LogLevel.WARNING -> RED
            } + s + RESET
    }

    companion object {

        private const val RESET = "\u001b[0m"
        private const val BLACK = "\u001b[0;30m"
        private const val RED = "\u001b[0;31m"
        private const val GREEN = "\u001b[0;32m"
        private const val YELLOW = "\u001b[0;33m"
        private const val BLUE = "\u001b[0;34m"



    }
}


