package com.soywiz.korau.sound

import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.browser.*
import kotlin.coroutines.*
import kotlin.coroutines.*

class MediaElementAudioSourceNodeWithAudioElement(
	val node: MediaElementAudioSourceNode,
	val audio: HTMLAudioElement
)

object HtmlSimpleSound {
	val ctx: BaseAudioContext? = try {
		when {
			jsTypeOf(window.asDynamic().AudioContext) != "undefined" -> AudioContext()
			jsTypeOf(window.asDynamic().webkitAudioContext) != "undefined" -> webkitAudioContext()
			else -> null
		}
	} catch (e: Throwable) {
		console.error(e)
		null
	}

	val available get() = ctx != null
	var unlocked = false
	private val unlockDeferred = CompletableDeferred<Unit>(Job())
	val unlock = unlockDeferred as Deferred<Unit>

	fun playSound(buffer: AudioBuffer): AudioBufferSourceNode? {
		if (ctx == null) return null
		val source = ctx.createBufferSource()
		source.buffer = buffer
		source.connect(ctx.destination)
		source.start(0.0)
		return source
	}

	fun stopSound(channel: AudioBufferSourceNode?) {
		channel?.disconnect(0)
		channel?.stop(0.0)
	}

	suspend fun waitUnlocked(): BaseAudioContext? {
		unlock.await()
		return ctx
	}

	fun callOnUnlocked(callback: (Unit) -> Unit): Cancellable {
		var cancelled = false
		unlock.invokeOnCompletion { if (!cancelled) callback(Unit) }
		return Cancellable { cancelled = true }
	}

	suspend fun loadSound(data: ArrayBuffer, url: String): AudioBuffer? {
		if (ctx == null) return null
		return suspendCoroutine<AudioBuffer> { c ->
			ctx.decodeAudioData(
				data,
				{ data -> c.resume(data) },
				{ c.resumeWithException(Exception("error decoding $url")) }
			)
		}
	}

	suspend fun loadSoundBuffer(url: String): MediaElementAudioSourceNodeWithAudioElement? {
		if (ctx == null) return null
		val audioPool = document.createElement("audio").unsafeCast<HTMLAudioElement>()
		audioPool.currentTime = 0.0
		audioPool.pause()
		audioPool.autoplay = false
		audioPool.src = url
		return MediaElementAudioSourceNodeWithAudioElement(ctx.createMediaElementSource(audioPool), audioPool)
	}

	suspend fun playSoundBuffer(buffer: MediaElementAudioSourceNodeWithAudioElement?) {
		if (ctx != null) {
			buffer?.audio?.play()
			buffer?.node?.connect(ctx.destination)
		}
	}

	suspend fun stopSoundBuffer(buffer: MediaElementAudioSourceNodeWithAudioElement?) {
		if (ctx != null) {
			buffer?.audio?.pause()
			buffer?.audio?.currentTime = 0.0
			buffer?.node?.disconnect(ctx.destination)
		}
	}

	suspend fun loadSound(data: ByteArray): AudioBuffer? = loadSound(data.unsafeCast<Int8Array>().buffer, "ByteArray")

	suspend fun loadSound(url: String): AudioBuffer? = loadSound(url.uniVfs.readBytes())

	init {
		val _scratchBuffer = ctx?.createBuffer(1, 1, 22050)
		lateinit var unlock: (e: Event) -> Unit
		unlock = {
			if (ctx != null) {
				val source = ctx.createBufferSource()

				source.buffer = _scratchBuffer
				source.connect(ctx.destination)
				source.start(0.0)
				if (jsTypeOf(ctx.asDynamic().resume) === "function") ctx.asDynamic().resume()
				source.onended = {
					source.disconnect(0)

					// Remove the touch start listener.
					document.removeEventListener("keydown", unlock, true)
					document.removeEventListener("touchstart", unlock, true)
					document.removeEventListener("touchend", unlock, true)
					document.removeEventListener("mousedown", unlock, true)

					unlocked = true
					unlockDeferred.complete(Unit)
				}
			}
		}

		document.addEventListener("keydown", unlock, true)
		document.addEventListener("touchstart", unlock, true)
		document.addEventListener("touchend", unlock, true)
		document.addEventListener("mousedown", unlock, true)
	}
}


open external class BaseAudioContext {
	fun createScriptProcessor(
		bufferSize: Int,
		numberOfInputChannels: Int,
		numberOfOutputChannels: Int
	): ScriptProcessorNode

	fun decodeAudioData(ab: ArrayBuffer, successCallback: (AudioBuffer) -> Unit, errorCallback: () -> Unit): Unit

	fun createMediaElementSource(audio: HTMLAudioElement): MediaElementAudioSourceNode
	fun createBufferSource(): AudioBufferSourceNode
	fun createBuffer(numOfchannels: Int, length: Int, rate: Int): AudioBuffer

	var currentTime: Double
	//var listener: AudioListener
	var sampleRate: Double
	var state: String
	val destination: AudioDestinationNode
}

external class AudioContext : BaseAudioContext
external class webkitAudioContext : BaseAudioContext

external interface MediaElementAudioSourceNode : AudioNode {

}

external interface AudioScheduledSourceNode : AudioNode {
	var onended: () -> Unit
	fun start(whn: Double = definedExternally, offset: Double = definedExternally, duration: Double = definedExternally)
	fun stop(whn: Double = definedExternally)
}

external interface AudioBufferSourceNode : AudioScheduledSourceNode {
	var buffer: AudioBuffer?
	var detune: Int
	var loop: Boolean
	var loopEnd: Double
	var loopStart: Double
	var playbackRate: Double
}

external interface AudioBuffer {
	val duration: Double
	val length: Double
	val numberOfChannels: Int
	val sampleRate: Int
	fun copyFromChannel(destination: Float32Array, channelNumber: Int, startInChannel: Double?): Unit
	fun copyToChannel(source: Float32Array, channelNumber: Int, startInChannel: Double?): Unit
	fun getChannelData(channel: Int): Float32Array
}

external interface AudioNode {
	val channelCount: Int
	//val channelCountMode: ChannelCountMode
	//val channelInterpretation: ChannelInterpretation
	val context: AudioContext
	val numberOfInputs: Int
	val numberOfOutputs: Int
	fun connect(destination: AudioNode, output: Int? = definedExternally, input: Int? = definedExternally): AudioNode
	//fun connect(destination: AudioParam, output: Int?): Unit
	fun disconnect(output: Int? = definedExternally): Unit

	fun disconnect(destination: AudioNode, output: Int? = definedExternally, input: Int? = definedExternally): Unit
	//fun disconnect(destination: AudioParam, output: Int?): Unit
}

external interface AudioDestinationNode : AudioNode {
	val maxChannelCount: Int
}

external class AudioProcessingEvent : Event {
	val inputBuffer: AudioBuffer
	val outputBuffer: AudioBuffer
	val playbackTime: Double
}

external interface ScriptProcessorNode : AudioNode {
	var onaudioprocess: (AudioProcessingEvent) -> Unit
}