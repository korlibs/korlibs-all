package com.soywiz.klogger

import com.soywiz.std.*

class Logger internal constructor(val name: String, val dummy: Boolean) {
	//@ThreadLocal // @TODO: kotlin-native bug https://github.com/JetBrains/kotlin-native/issues/1768
	companion object {
		private var loggers: Map<String, Logger> by atomicRef(mapOf())
		var defaultLevel: Level? by atomicRef(null)
		fun getLogger(name: String): Logger = loggers[name] ?: Logger(name, true)

		fun setLevel(name: String, level: Level) = getLogger(name).apply { this.level = level }
		fun setOutput(name: String, output: Output) = getLogger(name).apply { this.output = output }
		var defaultOutput: Output = ConsoleLogOutput
		operator fun invoke(name: String) = Logger.getLogger(name)
	}

	enum class Level(val index: Int) {
		NONE(0),
		FATAL(1),
		ERROR(2),
		WARN(3),
		INFO(4),
		DEBUG(5),
		TRACE(6)
	}

	interface Output {
		fun output(logger: Logger, level: Logger.Level, msg: Any?)
	}

	object ConsoleLogOutput : Logger.Output {
		override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
			//val line = "[${logger.name}]: $msg"
			when (level) {
				Logger.Level.ERROR -> Console.error(logger.name, msg)
				else -> Console.log(logger.name, msg)
			}
		}
	}

	init {
		//Logger.loggers = Logger.loggers + mapOf(name to this)
	}

	var level: Level? by atomicRef(null)
	var output: Output? by atomicRef(null)

	private val processedLevel: Level get() = level ?: Logger.defaultLevel ?: Level.WARN
	private val processedOutput: Output get() = output ?: Logger.defaultOutput

	@PublishedApi
	//@Deprecated("Keep for compatibility reasons since this was called from an inline", level = DeprecationLevel.HIDDEN)
	internal fun actualLog(level: Level, msg: Any?) {
		processedOutput.output(this, level, msg)
	}

	inline fun log(level: Level, msg: () -> Any?) {
		if (isEnabled(level)) {
			actualLog(level, msg())
		}
	}

	//inline fun logErrorInline(level: LogLevel, msg: () -> Any?) {
	//	if (isEnabled(level)) {
	//		KloggerConsole.error(name, msg())
	//		//actualLog(level, msg())
	//	}
	//}
	//
	//inline fun logInline(level: LogLevel, msg: () -> Any?) {
	//	if (isEnabled(level)) {
	//		KloggerConsole.log(name, msg())
	//		//actualLog(level, msg())
	//	}
	//}
	//
	//inline fun fatal(msg: () -> Any?) = logErrorInline(LogLevel.FATAL, msg)
	//inline fun error(msg: () -> Any?) = logErrorInline(LogLevel.ERROR, msg)
	//inline fun warn(msg: () -> Any?) = logInline(LogLevel.WARN, msg)
	//inline fun info(msg: () -> Any?) = logInline(LogLevel.INFO, msg)
	//inline fun debug(msg: () -> Any?) = logInline(LogLevel.DEBUG, msg)
	//inline fun trace(msg: () -> Any?) = logInline(LogLevel.TRACE, msg)

	inline fun fatal(msg: () -> Any?) = log(Level.FATAL, msg)
	inline fun error(msg: () -> Any?) = log(Level.ERROR, msg)
	inline fun warn(msg: () -> Any?) = log(Level.WARN, msg)
	inline fun info(msg: () -> Any?) = log(Level.INFO, msg)
	inline fun debug(msg: () -> Any?) = log(Level.DEBUG, msg)
	inline fun trace(msg: () -> Any?) = log(Level.TRACE, msg)

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

	fun isEnabled(level: Level) = level.index <= processedLevel.index

	inline val isFatalEnabled get() = isEnabled(Level.FATAL)
	inline val isErrorEnabled get() = isEnabled(Level.ERROR)
	inline val isWarnEnabled get() = isEnabled(Level.WARN)
	inline val isInfoEnabled get() = isEnabled(Level.INFO)
	inline val isDebugEnabled get() = isEnabled(Level.DEBUG)
	inline val isTraceEnabled get() = isEnabled(Level.TRACE)
}
