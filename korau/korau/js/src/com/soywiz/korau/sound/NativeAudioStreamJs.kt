package com.soywiz.korau.sound

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import org.khronos.webgl.*
import kotlin.browser.*
import kotlin.coroutines.*


actual val nativeSoundProvider: NativeSoundProvider by lazy { HtmlNativeSoundProvider() }

actual class NativeAudioStream actual constructor(val freq: Int) {
	val id = lastId++
	val logger = Logger("NativeAudioStream.js.$id")

	init {
		nativeSoundProvider.initOnce()
	}

	companion object {
		var lastId = 0
	}

	var missingDataCount = 0
	var nodeRunning = false
	var node: ScriptProcessorNode? = null

	var currentBuffer: MyNativeAudioBuffer? = null
	val buffers = Queue<MyNativeAudioBuffer>()

	private fun process(e: AudioProcessingEvent) {
		val left = e.outputBuffer.getChannelData(0)
		val right = e.outputBuffer.getChannelData(1)
		val sampleCount = left.length
		val hidden: Boolean = !!document.asDynamic().hidden
		var hasData = true

		for (n in 0 until sampleCount) {
			if (this.currentBuffer == null) {
				if (this.buffers.size == 0) {
					hasData = false
					break
				}
				this.currentBuffer = this.buffers.dequeue()
			}

			val cb = this.currentBuffer!!
			if (cb.available >= 2) {
				left[n] = cb.read()
				right[n] = cb.read()
				totalShorts -= 2
			} else {
				this.currentBuffer = null
				continue
			}

			if (hidden) {
				left[n] = 0f
				right[n] = 0f
			}
		}

		if (!hasData) {
			missingDataCount++
		}

		if (missingDataCount >= 500) {
			stop()
		}
	}

	private fun ensureInit() = run { node }

	private var startPromise: Cancellable? = null

	actual fun start() {
		if (nodeRunning) return
		startPromise = HtmlSimpleSound.callOnUnlocked {
			node = HtmlSimpleSound.ctx?.createScriptProcessor(1024, 2, 2)
			node?.onaudioprocess = { process(it) }
			if (HtmlSimpleSound.ctx != null) this.node?.connect(HtmlSimpleSound.ctx.destination)
		}
		missingDataCount = 0
		nodeRunning = true
	}

	actual fun stop() {
		if (!nodeRunning) return
		startPromise?.cancel()
		this.node?.disconnect()
		nodeRunning = false
	}

	fun ensureRunning() {
		ensureInit()
		if (!nodeRunning) {
			start()
		}
	}

	var totalShorts = 0
	actual val availableSamples get() = totalShorts

	actual suspend fun addSamples(samples: ShortArray, offset: Int, size: Int): Unit {
		//println("addSamples: $available, $size")
		//println(samples.sliceArray(offset until offset + size).toList())
		totalShorts += size
		if (!HtmlSimpleSound.available) {
			// Delay simulating consuming samples
			val sampleCount = (size / 2)
			val timeSeconds = sampleCount.toDouble() / 41_000.0
			coroutineContext.delay(timeSeconds.seconds)
		} else {
			ensureRunning()

			val fsamples = Float32Array(size)
			for (n in 0 until size) fsamples[n] = (samples[offset + n].toFloat() / Short.MAX_VALUE.toFloat()).toFloat()
			buffers.enqueue(MyNativeAudioBuffer(fsamples))

			while (buffers.size > 4) {
				coroutineContext.delayNextFrame()
			}
		}
	}
}

class MyNativeAudioBuffer(val data: Float32Array, var readedCallback: (() -> Unit)? = null) {
	var offset: Int = 0

	fun resolve() {
		val rc = readedCallback
		readedCallback = null
		rc?.invoke()
	}

	val hasMore: Boolean get() = this.offset < this.length

	fun read() = this.data[this.offset++]

	val available: Int get() = this.length - this.offset
	val length: Int get() = this.data.length
}
