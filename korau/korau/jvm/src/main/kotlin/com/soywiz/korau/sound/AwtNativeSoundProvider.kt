package com.soywiz.korau.sound

import com.soywiz.korau.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.stream.*
import java.io.*
import javax.sound.sampled.*
import javax.sound.sampled.AudioFormat

actual val nativeSoundProvider: NativeSoundProvider by lazy { AwtNativeSoundProvider() }

class AwtNativeSoundProvider : NativeSoundProvider() {
	override fun init(): Unit {
		AudioSystem.getMixerInfo()

		val af = AudioFormat(44100f, 16, 2, true, false)
		val info = DataLine.Info(SourceDataLine::class.java, af)
		val line = AudioSystem.getLine(info) as SourceDataLine

		line.open(af, 4096)
		line.start()
		line.write(ByteArray(4), 0, 4)
		line.drain()
		line.stop()
		line.close()
	}

	override suspend fun createSound(data: ByteArray, streaming: Boolean): NativeSound {
		try {
			return AwtNativeSound(
				(defaultAudioFormats.decode(data.openAsync()) ?: AudioData(44100, 2, shortArrayOf())).toWav()
			).init()
		} catch (e: Throwable) {
			e.printStackTrace()
			return AwtNativeSound(AudioData(44100, 2, shortArrayOf()).toWav()).init()
		}
		//return AwtNativeSound(data)
	}

	override suspend fun createSound(data: AudioData, formats: AudioFormats, streaming: Boolean): NativeSound {
		return AwtNativeSound(WAV.encodeToByteArray(data))
	}

	/*
	override suspend fun play(stream: BaseAudioStream): Unit = suspendCancellableCoroutine { c ->
		spawn(c.context) {
			executeInNewThread {
				val af = AudioFormat(stream.rate.toFloat(), 16, stream.channels, true, false)
				val info = DataLine.Info(SourceDataLine::class.java, af)
				val line = AudioSystem.getLine(info) as SourceDataLine

				line.open(af, 4096)
				line.start()

				val sdata = ShortArray(1024)
				val bdata = ByteArray(sdata.size * 2)
				//var writtenLength = 0L

				while (!c.cancelled) {
					//while (true) {
					//println(c.cancelled)
					//println(line.microsecondPosition)
					//println("" + line.longFramePosition + "/" + writtenLength + "/" + cancelled)
					val read = stream.read(sdata, 0, sdata.size)
					if (read <= 0) break
					var m = 0
					for (n in 0 until read) {
						val s = sdata[n].toInt()
						bdata[m++] = ((s ushr 0) and 0xFF).toByte()
						bdata[m++] = ((s ushr 8) and 0xFF).toByte()
					}
					//println(line.available())
					line.write(bdata, 0, m)
					//writtenLength += read / stream.channels
				}
				line.drain()
				line.stop()
				line.close()
				c.resume(Unit)
			}
		}
	}
	*/
}

class AwtNativeSound(val data: ByteArray) : NativeSound() {
	override var lengthInMs: Long = 0L

	suspend fun init(): AwtNativeSound {
		executeInWorker {
			val sound = AudioSystem.getAudioInputStream(ByteArrayInputStream(data))
			lengthInMs = (sound.frameLength * 1000.0 / sound.format.frameRate.toDouble()).toLong()
		}
		return this
	}

	override fun play(): NativeSoundChannel {
		return object : NativeSoundChannel(this) {
			val sound2 = AudioSystem.getAudioInputStream(ByteArrayInputStream(data))
			val info = DataLine.Info(Clip::class.java, sound2.format)
			val clip = AudioSystem.getLine(info) as Clip
			val len = clip.microsecondLength.toDouble() / 1_000_000.0
			override val current: Double get() = clip.microsecondPosition.toDouble() / 1_000_000.0
			override val total: Double get() = len
			override var playing: Boolean = true

			override fun stop() {
				clip.stop()
				playing = false
			}

			init {
				clip.open(sound2)
				clip.addLineListener(MyLineListener(clip) {
					stop()
				})
				clip.start()
			}
		}
	}

	private class MyLineListener(val clip: Clip, val complete: () -> Unit) : LineListener {
		override fun update(event: LineEvent) {
			when (event.type) {
				LineEvent.Type.STOP, LineEvent.Type.CLOSE -> {
					event.line.close()
					clip.removeLineListener(this)
					complete()
				}
			}
		}
	}
}