package com.soywiz.kmedialayer.scene

import com.soywiz.kmedialayer.*
import java.io.*
import kotlin.coroutines.experimental.*

public inline fun <T, R> Iterable<T>.firstNotNullOrNull(predicate: (T) -> R?): R? {
	for (e in this) {
		val res = predicate(e)
		if (res != null) return res
	}
	return null
}

class TestSceneApplication(val scene: Scene) : SceneApplication {
	var time = 0L

	inner class KmlTest : KmlBase() {
		override suspend fun loadFileBytes(path: String, range: LongRange?): ByteArray {
			return Kml.loadFileBytes(path, range)
		}

		override suspend fun delay(ms: Int) {
			// Do not delay
			step(ms)
		}

		override fun currentTimeMillis(): Double = time.toDouble()

		override suspend fun decodeImage(data: ByteArray): KmlNativeImageData {
			val reader = DataInputStream(ByteArrayInputStream(data))
			val magic = reader.readInt()
			if (magic == 0x89504E47.toInt()) { // PNG
				reader.skip(12)
				val width = reader.readInt()
				val height = reader.readInt()
				return object : KmlNativeImageData {
					override val width: Int = width
					override val height: Int = height
				}
			}
			throw IllegalArgumentException("Unknown image type with magic $magic")
		}
	}

	override suspend fun changeScene(scene: Scene) {
		TODO()
	}

	fun step(ms: Int) {
		val chunk = 16
		var elapsed = 0
		scene.apply {
			while (elapsed <= ms) {
				time += chunk
				//println("step: $chunk")
				updateScene(chunk)
				elapsed += chunk
			}
		}
	}

	override val kml: KmlBase = KmlTest()
	override val mouse = SceneApplication.Mouse()
}

fun <T : Scene> testSceneApplication(
	scene: T,
	windowConfig: WindowConfig = WindowConfig(),
	testBlock: suspend T.(TestSceneApplication) -> Unit
) {
	val application = TestSceneApplication(scene)
	scene.gl = KmlGlDummy
	scene.application = application
	runBlocking(CancellationToken(), {
		application.step(16)
	}) {
		scene.init()
		scene.testBlock(application)
	}
}

fun <T : Any> runBlocking(
	context: CoroutineContext = EmptyCoroutineContext,
	step: () -> Unit,
	callback: suspend () -> T
): T {
	var done = false
	lateinit var resultValue: T
	var resultException: Throwable? = null
	callback.startCoroutine(object : Continuation<T> {
		override val context: CoroutineContext = context
		override fun resume(value: T) {
			resultValue = value
			done = true
		}

		override fun resumeWithException(exception: Throwable) {
			println(exception)
			resultException = exception
			done = true
		}
	})
	while (!done) {
		step()
	}
	if (resultException != null) throw resultException!!
	return resultValue
}

