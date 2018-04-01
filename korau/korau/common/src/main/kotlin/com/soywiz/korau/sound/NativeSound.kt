package com.soywiz.korau.sound

import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.AudioStream
import com.soywiz.korau.format.defaultAudioFormats
import com.soywiz.korinject.AsyncDependency
import com.soywiz.korio.vfs.VfsFile

expect object NativeNativeSoundProvider {
	val instance: NativeSoundProvider
}

expect fun registerNativeSoundSpecialReader(): Unit

val nativeSoundProvider: NativeSoundProvider by lazy { NativeNativeSoundProvider.instance }

open class NativeSoundProvider : AsyncDependency {
	override suspend fun init(): Unit = Unit

	open suspend fun createSound(data: ByteArray): NativeSound = NativeSound()

	open suspend fun createSound(file: VfsFile): NativeSound = createSound(file.read())

	suspend open fun createSound(data: com.soywiz.korau.format.AudioData, formats: AudioFormats = defaultAudioFormats): NativeSound {
		return createSound(formats.encodeToByteArray(data))
	}

	suspend open fun play(stream: AudioStream): Unit = Unit
}

class DummyNativeSoundProvider : NativeSoundProvider() {
}

open class NativeSound {
	open val lengthInMs: Long = 0L

	suspend open fun play(): Unit {
	}
}

suspend fun VfsFile.readNativeSound() = nativeSoundProvider.createSound(this)
suspend fun VfsFile.readNativeSoundOptimized() = this.readSpecial(NativeSound::class)