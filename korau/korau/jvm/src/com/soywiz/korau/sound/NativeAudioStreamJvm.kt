package com.soywiz.korau.sound

import com.soywiz.kds.*
import com.soywiz.klogger.*
import com.soywiz.korio.async.*
import com.soywiz.std.*
import java.nio.*
import javax.sound.sampled.*
import kotlin.coroutines.*

data class SampleBuffer(val timestamp: Long, val data: ShortArray)

actual class NativeAudioStream actual constructor(val freq: Int) {
	companion object {
		var lastId = 0
		val mixer by lazy { AudioSystem.getMixer(null) }
	}

	val id = lastId++
	val logger = Logger("NativeAudioStream$id")
	val format by lazy { AudioFormat(freq.toFloat(), 16, 2, true, false) }
	var _msElapsed = 0.0
	val msElapsed get() = _msElapsed
	var totalShorts = 0
	val buffers = Queue<SampleBuffer>()
	var thread: Thread? = null
	var running = true

	val availableBuffers: Int get() = synchronized2(buffers) { buffers.size }
	val line by lazy { mixer.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine }

	actual val availableSamples get() = synchronized2(buffers) { totalShorts }

	fun ensureThread() {
		if (thread == null) {

			thread = Thread {
				line.open()
				line.start()
				logger.trace { "OPENED_LINE($id)!" }
				try {
					var timesWithoutBuffers = 0
					while (running) {
						while (availableBuffers > 0) {
							timesWithoutBuffers = 0
							val buf = synchronized2(buffers) { buffers.dequeue() }
							synchronized2(buffers) { totalShorts -= buf.data.size }
							val bdata = convertFromShortToByte(buf.data)

							val msChunk = (((bdata.size / 2) * 1000.0) / freq.toDouble()).toInt()

							_msElapsed += msChunk
							val now = System.currentTimeMillis()
							val latency = now - buf.timestamp
							//val drop = latency >= 150
							val start = System.currentTimeMillis()
							line.write(bdata, 0, bdata.size)
							//line.drain()
							val end = System.currentTimeMillis()
							logger.trace { "LINE($id): ${end - start} :: msChunk=$msChunk :: start=$start, end=$end :: available=${line.available()} :: framePosition=${line.framePosition} :: availableBuffers=$availableBuffers" }
						}
						logger.trace { "SHUT($id)!" }
						//Thread.sleep(500L) // 0.5 seconds of grace before shutting down this thread!
						Thread.sleep(50L) // 0.5 seconds of grace before shutting down this thread!
						timesWithoutBuffers++
						if (timesWithoutBuffers >= 10) break
					}
				} finally {
					logger.trace { "CLOSED_LINE($id)!" }
					line.stop()
					line.close()
				}

				thread = null
			}.apply {
				name = "NativeAudioStream$id"
				isDaemon = true
				start()
			}
		}
	}

	actual suspend fun addSamples(samples: ShortArray, offset: Int, size: Int) {
		val buffer = SampleBuffer(System.currentTimeMillis(), samples.copyOfRange(offset, offset + size))
		synchronized2(buffers) {
			totalShorts += buffer.data.size
			buffers.enqueue(buffer)
		}

		ensureThread()

		while (availableBuffers >= 5) {
			coroutineContext.delayNextFrame()
		}

		//val ONE_SECOND = 44100 * 2
		////val BUFFER_TIME_SIZE = ONE_SECOND / 8 // 1/8 second of buffer
		//val BUFFER_TIME_SIZE = ONE_SECOND / 4 // 1/4 second of buffer
		////val BUFFER_TIME_SIZE = ONE_SECOND / 2 // 1/2 second of buffer
		//while (bufferSize >= 32 && synchronized2(buffers) { totalShorts } > BUFFER_TIME_SIZE) {
		//	ensureThread()
		//	getCoroutineContext().eventLoop.sleepNextFrame()
		//}
	}

	fun convertFromShortToByte(sa: ShortArray, offset: Int = 0, size: Int = sa.size - offset): ByteArray {
		val bb = ByteBuffer.allocate(size * 2).order(ByteOrder.nativeOrder())
		val sb = bb.asShortBuffer()
		sb.put(sa, offset, size)
		return bb.array()
	}

	//suspend fun CoroutineContext.sleepImmediate2() = suspendCoroutine<Unit> { c ->
	//	eventLoop.setImmediate { c.resume(Unit) }
	//}
	actual fun stop() {
		running = false
	}

	actual fun start() {

	}
}
