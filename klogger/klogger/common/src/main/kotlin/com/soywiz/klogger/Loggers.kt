package com.soywiz.klogger

object Loggers {
    val loggers = LinkedHashMap<String, Logger>()
    var defaultLevel: LogLevel? = null

    fun getLogger(name: String) = loggers.getOrPut(name) { Logger(name, true) }

    fun setLevel(name: String, level: LogLevel) = getLogger(name).apply { this.level = level }

    fun setOutput(name: String, output: LogOutput) = getLogger(name).apply { this.output = output }

    var defaultOutput: LogOutput = ConsoleLogOutput
}
