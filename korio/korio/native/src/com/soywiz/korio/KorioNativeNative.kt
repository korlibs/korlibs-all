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

import kotlinx.cinterop.*
import platform.posix.*

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
	actual fun acquire(): Unit = Unit
	actual fun release(): Unit = Unit
}

object NativeDelay : Delay {
	override suspend fun delay(ms: Int): Unit = TODO()
}

actual val nativeDelay: Delay = NativeDelay

actual object KorioNative {
	actual val currentThreadId: Long get() = -1L // @TODO
	actual fun getClassSimpleName(clazz: KClass<*>): String = clazz.simpleName ?: "unknown"

	actual abstract class NativeThreadLocal<T> {
		actual abstract fun initialValue(): T
		private var value = initialValue()
		actual fun get(): T = value
		actual fun set(value: T) = run { this.value = value }
	}

	actual val platformName: String get() = "native"
	actual val rawOsName: String = "unknown"

	actual fun getRandomValues(data: ByteArray): Unit {
		for (n in 0 until data.size) {
			data[n] = platform.posix.rand().toByte()
		}
	}

	val tmpdir: String by lazy { getenv("TMPDIR") ?: getenv("TEMP") ?: getenv("TMP") ?: "/tmp" }

	val cwd by lazy {
		memScoped {
			val data = allocArray<ByteVar>(1024)
			getcwd(data, 1024)
			data.toKString()
		}
	}

	actual fun rootLocalVfs(): VfsFile = localVfs(cwd)
	actual fun applicationVfs(): VfsFile = localVfs(cwd)
	actual fun applicationDataVfs(): VfsFile = localVfs(cwd)
	actual fun cacheVfs(): VfsFile = MemoryVfs()
	actual fun externalStorageVfs(): VfsFile = localVfs(cwd)
	actual fun userHomeVfs(): VfsFile = localVfs(cwd)
	actual fun tempVfs(): VfsFile = localVfs(tmpdir)
	actual fun localVfs(path: String): VfsFile = LocalVfsNative(path).root
	actual val ResourcesVfs: VfsFile get() = applicationDataVfs()

	actual val File_separatorChar: Char get() = '/'

	actual val asyncSocketFactory: AsyncSocketFactory get() = NativeAsyncSocketFactory
	actual val websockets: WebSocketClientFactory get() = NativeWebSocketClientFactory
	actual val eventLoopFactoryDefaultImpl: EventLoopFactory get() = BaseEventLoopFactoryNative
	actual val systemLanguageStrings: List<String> get() = listOf("english")

	// @TODO
	actual suspend fun <T> executeInNewThread(callback: suspend () -> T): T = callback()

	actual suspend fun <T> executeInWorker(callback: suspend () -> T): T = callback()

	actual fun Thread_sleep(time: Long): Unit {
		platform.posix.usleep((time * 1000L).toInt())
	}

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
			override fun createClient(): HttpClient = NativeHttpClient()
			override fun createServer(): HttpServer = KorioHttpServer()
		}
	}

	actual fun enterDebugger(): Unit {
		println("enterDebugger")
	}

	actual fun printStackTrace(e: Throwable): Unit {
		println("Exception: $e")
	}

	actual fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
		sync(el = EventLoopTest(), step = 10, block = block)
	}

	actual fun getenv(key: String): String? = platform.posix.getenv(key)?.toKString()
}

class NativeHttpClient : HttpClient() {
	suspend override fun requestInternal(
		method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?
	): Response = TODO()
}

object NativeAsyncSocketFactory : AsyncSocketFactory() {
	override suspend fun createClient(): AsyncClient = TODO()
	override suspend fun createServer(port: Int, host: String, backlog: Int): AsyncServer = TODO()
}

object NativeWebSocketClientFactory : WebSocketClientFactory() {
	override suspend fun create(
		url: String,
		protocols: List<String>?,
		origin: String?,
		wskey: String?,
		debug: Boolean
	): WebSocketClient {
		TODO()
	}
}

class LocalVfsNative(val base: String) : LocalVfs() {
	val that = this
	override val absolutePath: String = ""

	fun resolve(path: String) = if (base.isEmpty()) path else "$base/" + path.trimStart('/')

	override suspend fun exec(
		path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler
	): Int = executeInWorker {
		TODO("LocalVfsNative.exec")
	}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val rpath = resolve(path)
		var fd = platform.posix.fopen(rpath, mode.cmode)

		if (fd == null) throw FileNotFoundException("Can't find '$rpath'")

		fun checkFd() {
			if (fd == null) error("Error with file '$rpath'")
		}

		return object : AsyncStreamBase() {
			override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				checkFd()
				//println("AsyncStreamBase.read($position, buffer=${buffer.size}, offset=$offset, len=$len)")
				return buffer.usePinned { pin ->
					if (len > 0) {
						platform.posix.fseek(fd, position.uncheckedCast(), platform.posix.SEEK_SET)
						platform.posix.fread(pin.addressOf(offset), 1, len.uncheckedCast(), fd).toInt()
					} else {
						0
					}
				}
			}

			override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
				checkFd()
				return buffer.usePinned { pin ->
					if (len > 0) {
						platform.posix.fseek(fd, position.uncheckedCast(), platform.posix.SEEK_SET)
						platform.posix.fwrite(pin.addressOf(offset), 1, len.uncheckedCast(), fd)
					}
					Unit
				}
			}

			override suspend fun setLength(value: Long): Unit {
				checkFd()
				platform.posix.truncate(rpath, value)
			}

			override suspend fun getLength(): Long {
				checkFd()
				platform.posix.fseek(fd, 0L, platform.posix.SEEK_END)
				return platform.posix.ftell(fd)
			}
			override suspend fun close() {
				checkFd()
				platform.posix.fclose(fd)
				fd = null
			}

			override fun toString(): String = "$that($path)"
		}.toAsyncStream()
	}

	override suspend fun setSize(path: String, size: Long): Unit = executeInWorker {
		platform.posix.truncate(resolve(path), size)
		Unit
	}

	override suspend fun stat(path: String): VfsStat = executeInWorker {
		val rpath = resolve(path)
		memScoped {
			val s = alloc<stat>()
			if (platform.posix.stat(rpath, s.ptr) == 0) {
				val size = s.st_size
				val isDirectory = (s.st_mode.toInt() and S_IFDIR) != 0
				createExistsStat(rpath, isDirectory, size)
			} else {
				createNonExistsStat(rpath)
			}
		}
	}

	override suspend fun list(path: String): SuspendingSequence<VfsFile> = executeInWorker {
		val dir = opendir(resolve(path))
		val out = ArrayList<VfsFile>()
		if (dir != null) {
			while (true) {
				val dent = readdir(dir) ?: break
				val name = dent.pointed.d_name.toKString()
				out += file(name)
			}
			closedir(dir)
		}
		SuspendingSequence(out)
	}

	override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean = executeInWorker {
		platform.posix.mkdir(resolve(path), "0777".toInt(8).uncheckedCast()) == 0
	}

	override suspend fun touch(path: String, time: Long, atime: Long): Unit = executeInWorker {
		// @TODO:
		println("TODO:LocalVfsNative.touch")
	}

	override suspend fun delete(path: String): Boolean = executeInWorker {
		platform.posix.unlink(resolve(path)) == 0
	}

	override suspend fun rmdir(path: String): Boolean = executeInWorker {
		platform.posix.rmdir(resolve(path)) == 0
	}

	override suspend fun rename(src: String, dst: String): Boolean = executeInWorker {
		platform.posix.rename(resolve(src), resolve(dst)) == 0
	}

	override suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable {
		// @TODO:
		println("TODO:LocalVfsNative.watch")
		return Closeable { }
	}

	override fun toString(): String = "LocalVfs"
}
