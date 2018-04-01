package com.soywiz.klogger

class Logger internal constructor(val name: String, val dummy: Boolean) {
	companion object {
		operator fun invoke(name: String) = Loggers.getLogger(name)
	}

	init {
		Loggers.loggers[name] = this
	}

	var level: LogLevel? = null
	var output: LogOutput? = null

	private val processedLevel: LogLevel get() = level ?: Loggers.defaultLevel ?: LogLevel.WARN
	private val processedOutput: LogOutput get() = output ?: Loggers.defaultOutput

	@PublishedApi
	//@Deprecated("Keep for compatibility reasons since this was called from an inline", level = DeprecationLevel.HIDDEN)
	internal fun actualLog(level: LogLevel, msg: String) {
		processedOutput.output(this, level, msg)
	}

	inline fun log(level: LogLevel, msg: () -> String) {
		if (isEnabled(level)) {
			actualLog(level, msg())
		}
	}

	inline fun fatal(msg: () -> String) = log(LogLevel.FATAL, msg)
	inline fun error(msg: () -> String) = log(LogLevel.ERROR, msg)
	inline fun warn(msg: () -> String) = log(LogLevel.WARN, msg)
	inline fun info(msg: () -> String) = log(LogLevel.INFO, msg)
	inline fun debug(msg: () -> String) = log(LogLevel.DEBUG, msg)
	inline fun trace(msg: () -> String) = log(LogLevel.TRACE, msg)

	@Deprecated("Potential performance problem. Use inline to lazily compute message.", ReplaceWith("fatal { msg }"))
	fun fatal(msg: String) = fatal { msg }

	@Deprecated("potential performance problem. Use inline to lazily compute message.", ReplaceWith("error { msg }"))
	fun error(msg: String) = error { msg }

	@Deprecated("potential performance problem. Use inline to lazily compute message.", ReplaceWith("warn { msg }"))
	fun warn(msg: String) = warn { msg }

	@Deprecated("potential performance problem. Use inline to lazily compute message.", ReplaceWith("info { msg }"))
	fun info(msg: String) = info { msg }

	@Deprecated("potential performance problem. Use inline to lazily compute message.", ReplaceWith("debug { msg }"))
	fun debug(msg: String) = debug { msg }

	@Deprecated("potential performance problem. Use inline to lazily compute message.", ReplaceWith("trace { msg }"))
	fun trace(msg: String) = trace { msg }

	fun isEnabled(level: LogLevel) = level.index <= processedLevel.index

	inline val isFatalEnabled get() = isEnabled(LogLevel.FATAL)
	inline val isErrorEnabled get() = isEnabled(LogLevel.ERROR)
	inline val isWarnEnabled get() = isEnabled(LogLevel.WARN)
	inline val isInfoEnabled get() = isEnabled(LogLevel.INFO)
	inline val isDebugEnabled get() = isEnabled(LogLevel.DEBUG)
	inline val isTraceEnabled get() = isEnabled(LogLevel.TRACE)
}
