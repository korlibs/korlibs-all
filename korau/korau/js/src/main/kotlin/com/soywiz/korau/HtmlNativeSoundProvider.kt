package com.soywiz.korau

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.NativeSoundProvider
import com.soywiz.korinject.AsyncDependency
import com.soywiz.korio.async.suspendCancellableCoroutine
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.browser.document

object HtmlNativeSoundProviderImpl {
	suspend fun createSound(data: ByteArray): NativeSound {
		// @TODO: This would produce leaks since no revokeObjectURL is called.
		val blob = Blob(arrayOf(data), BlobPropertyBag("audio/mp3"))
		val blobURL = URL.createObjectURL(blob)


		//return createFromUrl(blobURL.toJavaString())

		try {
			return createFromUrl(blobURL)
		} finally {
			URL.revokeObjectURL(blobURL)
		}
	}

	suspend fun createFromUrl(url: String) = HtmlNativeSound(url).apply { init() }
}

class HtmlNativeSoundProvider : NativeSoundProvider() {
	override suspend fun createSound(data: ByteArray): NativeSound = HtmlNativeSoundProviderImpl.createSound(data)
}

class HtmlNativeSound(val url: String) : NativeSound(), AsyncDependency {
	val audio = document.createElement("audio") as HTMLAudioElement
	val daudio: dynamic = audio
	//private val once = Once()

	override var lengthInMs: Long = 0L

	suspend override fun init() {
		initInternal()
		lengthInMs = (audio.duration * 1000L).toLong()
	}


	suspend fun initInternal() = suspendCancellableCoroutine<Unit> { c ->
		var ok: ((Event) -> Unit)? = null
		var error: ((Event) -> Unit)? = null

		fun removeEventListeners() {
			audio.removeEventListener("canplaythrough", ok)
			audio.removeEventListener("error", error)
			audio.removeEventListener("abort", error)
		}

		ok = {
			removeEventListeners()
			c.resume(Unit)

		}
		error = {
			removeEventListeners()
			c.resume(Unit)
		}

		audio.addEventListener("canplaythrough", ok)
		audio.addEventListener("error", error)
		audio.addEventListener("abort", error)

		c.onCancel {
			daudio.stop()
		}
	}

	suspend override fun play() = suspendCancellableCoroutine<Unit> { c ->
		var done: ((Event) -> Unit)? = null

		fun removeEventListeners() {
			audio.removeEventListener("ended", done)
			audio.removeEventListener("pause", done)
			audio.removeEventListener("stalled", done)
			audio.removeEventListener("error", done)
		}

		done = {
			removeEventListeners()
			c.resume(Unit)
		}

		audio.addEventListener("ended", done)
		audio.addEventListener("pause", done)
		audio.addEventListener("stalled", done)
		audio.addEventListener("error", done)
		audio.play()

		c.onCancel {
			daudio.stop()
		}
	}
}