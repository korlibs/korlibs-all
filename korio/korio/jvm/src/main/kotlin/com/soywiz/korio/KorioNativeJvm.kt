package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.net.ws.*
import com.soywiz.korio.vfs.*
import java.io.*
import java.security.*
import javax.crypto.*
import javax.crypto.spec.*
import kotlin.reflect.*


actual typealias Synchronized = kotlin.jvm.Synchronized
actual typealias JvmField = kotlin.jvm.JvmField
actual typealias JvmStatic = kotlin.jvm.JvmStatic
actual typealias JvmOverloads = kotlin.jvm.JvmOverloads
actual typealias Transient = kotlin.jvm.Transient

actual typealias Language = org.intellij.lang.annotations.Language

actual typealias IOException = java.io.IOException
actual typealias EOFException = java.io.EOFException
actual typealias FileNotFoundException = java.io.FileNotFoundException

actual typealias RuntimeException = java.lang.RuntimeException
actual typealias IllegalStateException = java.lang.IllegalStateException
actual typealias CancellationException = java.util.concurrent.CancellationException

actual class Semaphore actual constructor(initial: Int) {
	val jsema = java.util.concurrent.Semaphore(initial)
	//var initial: Int
	actual fun acquire() = jsema.acquire()

	actual fun release() = jsema.release()
}

actual object KorioNative {
	actual val currentThreadId: Long get() = Thread.currentThread().id

	actual fun getClassSimpleName(clazz: KClass<*>): String = clazz.java.name

	actual abstract class NativeThreadLocal<T> {
		actual abstract fun initialValue(): T

		val jthreadLocal = object : ThreadLocal<T>() {
			override fun initialValue(): T = this@NativeThreadLocal.initialValue()
		}

		actual fun get(): T = jthreadLocal.get()
		actual fun set(value: T) = jthreadLocal.set(value)
	}

	suspend private fun <T> _executeInside(task: suspend () -> T, executionScope: (body: () -> Unit) -> Unit): T {
		val deferred = Promise.Deferred<T>()
		val parentEventLoop = eventLoop()
		tasksInProgress.incrementAndGet()
		executionScope {
			syncTest {
				try {
					val res = task()
					parentEventLoop.queue {
						deferred.resolve(res)
					}
				} catch (e: Throwable) {
					parentEventLoop.queue { deferred.reject(e) }
				} finally {
					tasksInProgress.decrementAndGet()
				}
			}
		}
		return deferred.promise.await()
	}

	actual suspend fun <T> executeInNewThread(callback: suspend () -> T): T = _executeInside(callback) { body ->
		Thread {
			body()
		}.apply {
			isDaemon = true
			start()
		}
	}

	actual suspend fun <T> executeInWorker(callback: suspend () -> T): T = _executeInside(callback) { body ->
		Thread {
			body()
		}.apply {
			isDaemon = true
			start()
		}
	}

	//actual suspend fun <T> executeInWorker(callback: suspend () -> T): T = _executeInside(callback) { body ->
	//	workerLazyPool.executeUpdatingTasksInProgress {
	//		body()
	//	}
	//}

	actual val platformName: String = "jvm"
	actual val rawOsName: String by lazy { System.getProperty("os.name") }

	private val secureRandom: SecureRandom by lazy { SecureRandom.getInstanceStrong() }

	actual fun getRandomValues(data: ByteArray): Unit {
		secureRandom.nextBytes(data)
	}

	actual val httpFactory: HttpFactory by lazy {
		object : HttpFactory {
			init {
				System.setProperty("http.keepAlive", "false")
			}

			override fun createClient(): HttpClient = HttpClientJvm()
			override fun createServer(): HttpServer = KorioNativeDefaults.createServer()
		}
	}

	actual class SimplerMessageDigest actual constructor(name: String) {
		val md = MessageDigest.getInstance(name)

		actual suspend fun update(data: ByteArray, offset: Int, size: Int) =
			executeInWorker { md.update(data, offset, size) }

		actual suspend fun digest(): ByteArray = executeInWorker { md.digest() }
	}

	actual class SimplerMac actual constructor(name: String, key: ByteArray) {
		val mac = Mac.getInstance(name).apply { init(SecretKeySpec(key, name)) }
		actual suspend fun update(data: ByteArray, offset: Int, size: Int) =
			executeInWorker { mac.update(data, offset, size) }

		actual suspend fun finalize(): ByteArray = executeInWorker { mac.doFinal() }
	}

	actual fun Thread_sleep(time: Long) = Thread.sleep(time)

	actual val eventLoopFactoryDefaultImpl: EventLoopFactory = EventLoopFactoryJvmAndCSharp()

	actual val asyncSocketFactory: AsyncSocketFactory by lazy { JvmAsyncSocketFactory() }
	actual val websockets: WebSocketClientFactory get() = TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	actual val File_separatorChar: Char by lazy { File.separatorChar }

	actual fun rootLocalVfs(): VfsFile = localVfs(".")
	actual fun applicationVfs(): VfsFile = localVfs(File(".").absolutePath)
	actual fun applicationDataVfs(): VfsFile = localVfs(File(".").absolutePath)
	actual fun cacheVfs(): VfsFile = MemoryVfs()
	actual fun externalStorageVfs(): VfsFile = localVfs(".")
	actual fun userHomeVfs(): VfsFile = localVfs(".")
	actual fun tempVfs(): VfsFile = localVfs(tmpdir)
	actual fun localVfs(path: String): VfsFile = LocalVfsJvm()[path]

	actual val ResourcesVfs: VfsFile by lazy { ResourcesVfsProviderJvm()().root }

	val tmpdir: String get() = System.getProperty("java.io.tmpdir")

	actual fun enterDebugger() = Unit
	actual fun printStackTrace(e: Throwable) = e.printStackTrace()
	actual fun log(msg: Any?): Unit = java.lang.System.out.println(msg)
	actual fun error(msg: Any?): Unit = java.lang.System.err.println(msg)

	actual fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
		sync(el = EventLoopTest(), step = 10, block = block)
	}
}
