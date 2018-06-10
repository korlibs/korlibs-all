package com.soywiz.korio

import com.soywiz.klogger.*
import com.soywiz.korio.async.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.net.ws.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.file.*
import kotlin.coroutines.experimental.*
import kotlin.math.*
import kotlin.reflect.*

expect annotation class Synchronized()
expect annotation class JvmField()
expect annotation class JvmStatic()
expect annotation class JvmOverloads()
expect annotation class Transient()

annotation class Language(val value: String, val prefix: String = "", val suffix: String = "")
//expect annotation class Language(val value: String, val prefix: String = "", val suffix: String = "")

expect open class IOException(msg: String) : Exception
expect open class EOFException(msg: String) : IOException
expect open class FileNotFoundException(msg: String) : IOException

expect open class RuntimeException(msg: String) : Exception
expect open class IllegalStateException(msg: String) : RuntimeException
expect open class CancellationException(msg: String) : IllegalStateException

expect class Semaphore(initial: Int) {
	//var initial: Int
	fun acquire()

	fun release()
}

expect val nativeDelay: Delay

expect object KorioNative {
	abstract class NativeThreadLocal<T>() {
		abstract fun initialValue(): T
		fun get(): T
		fun set(value: T): Unit
	}

	fun getClassSimpleName(clazz: KClass<*>): String

	val currentThreadId: Long
	val platformName: String
	val rawOsName: String
	val ResourcesVfs: VfsFile

	val websockets: WebSocketClientFactory

	val eventLoopFactoryDefaultImpl: EventLoopFactory

	val systemLanguageStrings: List<String>

	fun getRandomValues(data: ByteArray): Unit

	suspend fun <T> executeInNewThread(callback: suspend () -> T): T
	suspend fun <T> executeInWorker(callback: suspend () -> T): T

	val File_separatorChar: Char

	fun rootLocalVfs(): VfsFile
	fun applicationVfs(): VfsFile
	fun applicationDataVfs(): VfsFile
	fun cacheVfs(): VfsFile
	fun externalStorageVfs(): VfsFile
	fun userHomeVfs(): VfsFile
	fun localVfs(path: String): VfsFile
	fun tempVfs(): VfsFile

	fun Thread_sleep(time: Long): Unit

	val asyncSocketFactory: AsyncSocketFactory

	val httpFactory: HttpFactory

	fun printStackTrace(e: Throwable)
	fun enterDebugger()

	class SimplerMessageDigest(name: String) {
		suspend fun update(data: ByteArray, offset: Int, size: Int): Unit
		suspend fun digest(): ByteArray
	}

	class SimplerMac(name: String, key: ByteArray) {
		suspend fun update(data: ByteArray, offset: Int, size: Int)
		suspend fun finalize(): ByteArray
	}

	fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit

	fun getenv(key: String): String?
}

object KorioNativeDefaults {
	fun printStackTrace(e: Throwable) {
		Logger("KorioNativeDefaults").error { "printStackTrace:" }
		Logger("KorioNativeDefaults").error { e.message ?: "Error" }
	}

	fun createServer(): HttpServer {
		val HeaderRegex = Regex("^(\\w+)\\s+(.*)\\s+(HTTP/1.[01])$")

		return object : HttpServer() {
			val BodyChunkSize = 1024
			val LimitRequestFieldSize = 8190
			val LimitRequestFields = 100

			var wshandler: suspend (WsRequest) -> Unit = {}
			var handler: suspend (Request) -> Unit = {}
			val onClose = Signal<Unit>()
			override var actualPort: Int = -1; private set

			override suspend fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
				this.wshandler = handler
			}

			override suspend fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
				this.handler = handler
			}

			override suspend fun listenInternal(port: Int, host: String) {
				val socket = KorioNative.asyncSocketFactory.createServer(port, host)
				actualPort = socket.port
				tasksInProgress.incrementAndGet()
				val close = socket.listen { client ->
					while (true) {
						//println("Connected! : $client : ${KorioNative.currentThreadId}")
						val cb = client.toBuffered()
						//val cb = client

						//val header = cb.readBufferedLine().trim()
						//val fline = cb.readBufferedUntil('\n'.toByte()).toString(UTF8).trim()
						val fline = cb.readUntil('\n'.toByte(), limit = LimitRequestFieldSize).toString(UTF8).trim()
						//println("fline: $fline")
						val match = HeaderRegex.matchEntire(fline)
								?: throw IllegalStateException("Not a valid request '$fline'")
						val method = match.groupValues[1]
						val url = match.groupValues[2]
						val httpVersion = match.groupValues[3]
						val headerList = arrayListOf<Pair<String, String>>()
						for (n in 0 until LimitRequestFields) { // up to 1024 headers
							val line = cb.readUntil('\n'.toByte(), limit = LimitRequestFieldSize).toString(UTF8).trim()
							if (line.isEmpty()) break
							val parts = line.split(':', limit = 2)
							headerList += parts.getOrElse(0) { "" }.trim() to parts.getOrElse(1) { "" }.trim()
						}
						val headers = Http.Headers(headerList)
						val keepAlive = headers["connection"]?.toLowerCase() == "keep-alive"
						val contentLength = headers["content-length"]?.toLongOrNull()

						//println("REQ: $method, $url, $headerList")

						val requestCompleted = Promise.Deferred<Unit>()

						var bodyHandler: (ByteArray) -> Unit = {}
						var endHandler: () -> Unit = {}

						spawnAndForget {
							handler(object : HttpServer.Request(Http.Method(method), url, headers) {
								override suspend fun _handler(handler: (ByteArray) -> Unit) =
									run { bodyHandler = handler }

								override suspend fun _endHandler(handler: () -> Unit) = run { endHandler = handler }

								override suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers) {
									val sb = StringBuilder()
									sb.append("$httpVersion $code $message\r\n")
									for (header in headers) sb.append("${header.first}: ${header.second}\r\n")
									sb.append("\r\n")
									client.write(sb.toString().toByteArray(UTF8))
								}

								override suspend fun _write(data: ByteArray, offset: Int, size: Int) {
									client.write(data, offset, size)
								}

								override suspend fun _end() {
									requestCompleted.resolve(Unit)
								}
							})
						}

						//println("Content-Length: '${headers["content-length"]}'")
						//println("Content-Length: $contentLength")
						if (contentLength != null) {
							var remaining = contentLength
							while (remaining > 0) {
								val toRead = min(BodyChunkSize.toLong(), remaining).toInt()
								val read = cb.readBytesUpToFirst(toRead)
								bodyHandler(read)
								remaining -= read.size
							}
						}
						endHandler()

						requestCompleted.promise.await()

						if (keepAlive) continue

						client.close()
						break
					}
				}

				onClose {
					close.close()
					tasksInProgress.decrementAndGet()
				}
			}

			override suspend fun closeInternal() {
				onClose()
			}
		}
	}
}

fun createBase64URLForData(data: ByteArray, contentType: String): String {
	return "data:$contentType;base64," + Base64.encode(data)
}

interface Delay : CoroutineContext.Element {
	object KEY : CoroutineContext.Key<Delay> {}

	override val key get() = KEY
	suspend fun delay(ms: Int): Unit
}

val CoroutineContext.delay: Delay get() = this[Delay.KEY]?.delay ?: nativeDelay
