package com.soywiz.korau.sound

import com.soywiz.klock.*
import com.soywiz.korau.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import kotlinx.coroutines.*

expect val nativeSoundProvider: NativeSoundProvider

open class NativeSoundProvider {
	private var initialized = false

	open fun initOnce() {
		if (!initialized) {
			initialized = true
			init()
		}
	}

	protected open fun init(): Unit = Unit

	open suspend fun createSound(data: ByteArray, streaming: Boolean = false): NativeSound = object : NativeSound() {
		override fun play(): NativeSoundChannel = object : NativeSoundChannel(this) {
			override fun stop() {
			}
		}
	}

	open suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean = false): NativeSound =
		createSound(vfs.file(path).read(), streaming)

	suspend fun createSound(file: FinalVfsFile, streaming: Boolean = false): NativeSound =
		createSound(file.vfs, file.path, streaming)

	suspend fun createSound(file: VfsFile, streaming: Boolean = false): NativeSound =
		createSound(file.getUnderlyingUnscapedFile(), streaming)

	open suspend fun createSound(
		data: com.soywiz.korau.format.AudioData,
		formats: AudioFormats = defaultAudioFormats,
		streaming: Boolean = false
	): NativeSound {
		return createSound(WAV.encodeToByteArray(data), streaming)
	}

	open suspend fun play(stream: BaseAudioStream, bufferSeconds: Double = 0.1): Unit =
		suspendCancellableCoroutine<Unit> { c ->
			val nas = NewNativeAudioStream()
			val task = asyncImmediately(c.context) {
				val temp = ShortArray(1024)
				val nchannels = 2
				val minBuf = stream.rate * nchannels * bufferSeconds
				while (true) {
					val read = stream.read(temp, 0, temp.size)
					nas.addSamples(temp, 0, read)
					while (nas.availableSamples in minBuf..minBuf * 2) c.context.delayNextFrame() // 100ms of buffering, and 1s as much
				}
			}
			nas.start()
			c.invokeOnCancellation {
				task.cancel()
				nas.stop()
			}
		}
}

class DummyNativeSoundProvider : NativeSoundProvider()

abstract class NativeSoundChannel(val sound: NativeSound) {
	private val startTime = Klock.currentTimeMillisDouble()
	open var volume = 1.0
	open val current: Double get() = Klock.currentTimeMillisDouble() - startTime
	open val total: Double get() = sound.lengthInSeconds
	open val playing get() = current < total
	abstract fun stop(): Unit
	suspend fun await(progress: (current: Double, total: Double) -> Unit = { current, total -> }) {
		suspendCancellableCoroutine<Unit> { c ->
			launchImmediately(c.context) {
				try {
					while (playing) {
						progress(current, total)
						c.context.delayNextFrame()
					}
					progress(total, total)
				} catch (e: CancellationException) {
					stop()
				}
			}
			c.invokeOnCancellation {
				stop()
			}
		}
	}
}

abstract class NativeSound {
	open val lengthInMs: Long = 0L
	abstract fun play(): NativeSoundChannel
	suspend fun playAndWait(progress: (current: Double, total: Double) -> Unit = { current, total -> }): Unit =
		play().await(progress)
}

val NativeSound.lengthInSeconds: Double get() = lengthInMs.toDouble() / 1000.0

suspend fun VfsFile.readNativeSound(streaming: Boolean = false) = nativeSoundProvider.createSound(this, streaming)
suspend fun VfsFile.readNativeSoundOptimized(streaming: Boolean = false) =
	nativeSoundProvider.createSound(this, streaming)
