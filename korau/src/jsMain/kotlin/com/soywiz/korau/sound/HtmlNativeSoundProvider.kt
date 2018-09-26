package com.soywiz.korau.sound

import com.soywiz.klogger.*
import com.soywiz.korio.async.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

class HtmlNativeSoundProvider : NativeSoundProvider() {
	private val log = Logger("HtmlNativeSoundProvider")

	override fun initOnce() {
	}

	override suspend fun createSound(data: ByteArray, streaming: Boolean): NativeSound {
		return AudioBufferNativeSound(HtmlSimpleSound.loadSound(data))
		/*
		return if (streaming) {
			createTemporalURLForData(data, "audio/mp3") { url ->
				MediaNativeSound(url)
			}
			//return MediaNativeSound(createURLForData(data, "audio/mp3")) // @TODO: Leak
			//return MediaNativeSound(createBase64URLForData(data, "audio/mp3"))
		} else {
			AudioBufferNativeSound(HtmlSimpleSound.loadSound(data))
		}
		*/
	}

	override suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean): NativeSound = when (vfs) {
		is LocalVfs, is UrlVfs -> {
			val rpath = when (vfs) {
				is LocalVfs -> {
					log.trace { "LOCAL: HtmlNativeSoundSpecialReader: $vfs, $path" }
					path
				}
				is UrlVfs -> {
					log.trace { "URL: HtmlNativeSoundSpecialReader: $vfs, $path" }
					vfs.getFullUrl(path)
				}
				else -> invalidOp
			}
			//if (streaming) {
			//	MediaNativeSound(rpath)
			//} else {
			//	AudioBufferNativeSound(HtmlSimpleSound.loadSound(rpath))
			//}
			AudioBufferNativeSound(HtmlSimpleSound.loadSound(rpath))
		}
		else -> {
			log.trace { "OTHER: HtmlNativeSoundSpecialReader: $vfs, $path" }
			super.createSound(vfs, path)
		}
	}
}

class MediaNativeSound private constructor(
	val context: CoroutineContext,
	val url: String,
	override val lengthInMs: Long
) : NativeSound() {
	companion object {
		val log = Logger("MediaNativeSound")

		suspend operator fun invoke(url: String): NativeSound {
			//val audio = document.createElement("audio").unsafeCast<HTMLAudioElement>()
			//audio.autoplay = false
			//audio.src = url
			return MediaNativeSound(coroutineContext, url, 100L)
			//val audio = document.createElement("audio").unsafeCast<HTMLAudioElement>()
			//audio.autoplay = false
			//audio.src = url
			//log.trace { "CREATE SOUND FROM URL: $url" }
			//
			//suspendCancellableCoroutine<Unit> { c ->
			//	var ok: ((Event) -> Unit)? = null
			//	var error: ((Event) -> Unit)? = null
			//
			//	fun removeEventListeners() {
			//		audio.removeEventListener("canplaythrough", ok)
			//		audio.removeEventListener("error", error)
			//		audio.removeEventListener("abort", error)
			//	}
			//
			//	ok = {
			//		log.trace { "OK" }
			//		removeEventListeners()
			//		c.resume(Unit)
			//
			//	}
			//	error = {
			//		log.trace { "ERROR" }
			//		removeEventListeners()
			//		c.resume(Unit)
			//	}
			//
			//	audio.addEventListener("canplaythrough", ok)
			//	audio.addEventListener("error", error)
			//	audio.addEventListener("abort", error)
			//}
			//log.trace { "DURATION_MS: ${(audio.duration * 1000).toLong()}" }
			//return MediaNativeSound(url, (audio.duration * 1000).toLong())
		}
	}

	override fun play(): NativeSoundChannel {
		return object : NativeSoundChannel(this) {
			val bufferPromise = asyncImmediately(context) {
				if (HtmlSimpleSound.unlocked) HtmlSimpleSound.loadSoundBuffer(url) else null
			}
			val channelPromise = asyncImmediately(context) {
				val buffer = bufferPromise.await()
				if (buffer != null) HtmlSimpleSound.playSoundBuffer(buffer) else null
			}

			override fun stop() {
				launchImmediately(context) {
					val res = bufferPromise.await()
					if (res != null) HtmlSimpleSound.stopSoundBuffer(res)
				}
			}
		}
	}
}

class AudioBufferNativeSound(val buffer: AudioBuffer?) : NativeSound() {
	override val lengthInMs: Long = (buffer?.length?.times(1000))?.toLong() ?: 0L

	override fun play(): NativeSoundChannel {
		return object : NativeSoundChannel(this) {
			val channel = if (buffer != null) HtmlSimpleSound.playSound(buffer) else null
			override fun stop() {
				channel?.stop()
			}
		}
	}
}

private suspend fun soundProgress(
	totalTime: Double,
	timeProvider: () -> Double,
	progress: (Double, Double) -> Unit,
	startTime: Double = timeProvider()
) {
	while (true) {
		val now = timeProvider()
		val elapsed = now - startTime
		if (elapsed >= totalTime) break
		progress(elapsed, totalTime)
		delayNextFrame()
	}
	progress(totalTime, totalTime)
}
