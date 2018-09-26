package com.soywiz.korau

import android.media.*
import com.soywiz.kds.*
import com.soywiz.korau.format.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*

class AndroidNativeSoundProvider : NativeSoundProvider() {
	val mpPool = Pool(reset = {
		it.setOnCompletionListener(null)
		it.reset()
	}) { MediaPlayer() }

	fun getDurationInMs(url: String): Int {
		return mpPool.alloc { mp ->
			mp.setDataSource(url)
			mp.prepare()
			mp.duration
		}
	}

	override suspend fun createSound(data: ByteArray, streaming: Boolean): NativeSound =
		AndroidNativeSound(this, "data:audio/mp3;base64," + Base64.encode(data))
	//suspend override fun createSound(file: VfsFile): NativeSound {
	//}

	override suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean): NativeSound {
		return try {
			when (vfs) {
				is LocalVfs -> AndroidNativeSound(this, path)
				else -> super.createSound(vfs, path, streaming)
			}
		} catch (e: Throwable) {
			e.printStackTrace()
			nativeSoundProvider.createSound(AudioData(44100, 2, shortArrayOf()))
		}
	}
}

class AndroidNativeSound(val prov: AndroidNativeSoundProvider, val url: String) : NativeSound() {
	override val lengthInMs: Long by lazy { prov.getDurationInMs(url).toLong() }

	override fun play(): NativeSoundChannel {
		var mp: MediaPlayer? = prov.mpPool.alloc()
		return object : NativeSoundChannel(this) {
			override val current: Double = mp?.currentPosition?.toDouble() ?: 0.0
			override val total: Double = mp?.duration?.toDouble() ?: 0.0
			override var playing: Boolean = true

			override fun stop() {
				playing = false
				if (mp != null) prov.mpPool.free(mp!!)
				mp = null
			}

			init {
				mp?.setDataSource(url)
				mp?.setOnCompletionListener {
					stop()
				}
				mp?.prepare()
				mp?.start()
			}
		}
	}
}