package Utility

import java.io.{File, FileWriter, PrintWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Logger(name: String) {
    private val logsDir = new File("logs")
    if (!logsDir.exists()) {
        logsDir.mkdir()
    }

    private val logFile = new File(s"logs/$name.log")
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private lazy val writer: PrintWriter = {
        new PrintWriter(new FileWriter(logFile, true))
    }

    private def log(level: String, message: String): Unit = {
        val timestamp = LocalDateTime.now().format(dateFormat)
        val logMessage = s"[$timestamp] [$level] [$name] $message"

        println(logMessage)

        this.synchronized {
            writer.println(logMessage)
            writer.flush()
        }
    }

    def info(message: String): Unit = log("INFO", message)
    def warn(message: String): Unit = log("WARN", message)
    def error(message: String): Unit = log("ERROR", message)
    def debug(message: String): Unit = log("DEBUG", message)

    def close(): Unit = {
        if (writer != null) {
            writer.close()
        }
    }
}

object Logger {
    def apply(name: String): Logger = new Logger(name)
}
