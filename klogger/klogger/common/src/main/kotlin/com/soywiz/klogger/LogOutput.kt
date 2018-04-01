package com.soywiz.klogger

interface LogOutput {
    fun output(logger: Logger, level: LogLevel, msg: String)
}

object ConsoleLogOutput : LogOutput {
    override fun output(logger: Logger, level: LogLevel, msg: String) {
        val line = "[${logger.name}]: $msg"
        when (level) {
            LogLevel.ERROR -> KloggerConsole.error(line)
            else -> KloggerConsole.log(line)
        }
    }

}
