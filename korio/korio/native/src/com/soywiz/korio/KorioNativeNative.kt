package com.soywiz.korio

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.net.ws.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlin.collections.set
import kotlin.reflect.*
import kotlin.coroutines.experimental.*

actual annotation class Synchronized
actual annotation class JvmField
actual annotation class JvmStatic
actual annotation class JvmOverloads
actual annotation class Transient

actual open class IOException actual constructor(msg: String) : Exception(msg)
actual open class EOFException actual constructor(msg: String) : IOException(msg)
actual open class FileNotFoundException actual constructor(msg: String) : IOException(msg)

actual open class RuntimeException actual constructor(msg: String) : Exception(msg)
actual open class IllegalStateException actual constructor(msg: String) : RuntimeException(msg)
actual open class CancellationException actual constructor(msg: String) : IllegalStateException(msg)

actual class Semaphore actual constructor(initial: Int) {
	actual fun acquire(): Unit = TODO()
	actual fun release(): Unit = TODO()
}

object NativeDelay : Delay {
	override suspend fun delay(ms: Int): Unit = TODO()
}

actual val nativeDelay: Delay = NativeDelay

actual object KorioNative {
	actual val currentThreadId: Long get() = TODO()
	actual fun getClassSimpleName(clazz: KClass<*>): String = clazz.simpleName ?: "unknown"

	actual abstract class NativeThreadLocal<T> {
		actual abstract fun initialValue(): T
		private var value = initialValue()
		actual fun get(): T = value
		actual fun set(value: T) = run { this.value = value }
	}

	actual val platformName: String get() = "native"
	actual val rawOsName: String = "unknown"

	actual fun getRandomValues(data: ByteArray): Unit = TODO()

	actual fun rootLocalVfs(): VfsFile = TODO()
	actual fun applicationVfs(): VfsFile = TODO()
	actual fun applicationDataVfs(): VfsFile = TODO()
	actual fun cacheVfs(): VfsFile = TODO()
	actual fun externalStorageVfs(): VfsFile = TODO()
	actual fun userHomeVfs(): VfsFile = TODO()
	actual fun tempVfs(): VfsFile = TODO()
	actual fun localVfs(path: String): VfsFile = TODO()
	actual val File_separatorChar: Char get() = TODO()
	actual val asyncSocketFactory: AsyncSocketFactory get() = TODO()
	actual val websockets: WebSocketClientFactory get() = TODO()
	actual val eventLoopFactoryDefaultImpl: EventLoopFactory = TODO()
	actual val systemLanguageStrings: List<String> get() = TODO()
	actual suspend fun <T> executeInNewThread(callback: suspend () -> T): T = TODO()
	actual suspend fun <T> executeInWorker(callback: suspend () -> T): T = TODO()
	actual fun Thread_sleep(time: Long): Unit = TODO()
	actual class SimplerMessageDigest actual constructor(name: String) {
		actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
		actual suspend fun digest(): ByteArray = TODO()
	}

	actual class SimplerMac actual constructor(name: String, key: ByteArray) {
		actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
		actual suspend fun finalize(): ByteArray = TODO()
	}

	actual val httpFactory: HttpFactory by lazy {
		object : HttpFactory {
			override fun createClient(): HttpClient = TODO()
			override fun createServer(): HttpServer = TODO()
		}
	}

	actual val ResourcesVfs: VfsFile get() = TODO()
	actual fun enterDebugger(): Unit = TODO()
	actual fun printStackTrace(e: Throwable): Unit = TODO()
	actual fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit = TODO()
	actual fun getenv(key: String): String? = TODO()
}
