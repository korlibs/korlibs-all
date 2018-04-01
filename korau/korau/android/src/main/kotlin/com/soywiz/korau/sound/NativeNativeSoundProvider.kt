package com.soywiz.korau.sound

import com.soywiz.korau.AndroidNativeSoundProvider
import com.soywiz.korau.format.AudioData
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsSpecialReader
import com.soywiz.korio.vfs.register

actual object NativeNativeSoundProvider {
	actual val instance: NativeSoundProvider by lazy { AndroidNativeSoundProvider() }
}

actual fun registerNativeSoundSpecialReader(): Unit {
	AndroidNativeSoundSpecialReader().register()
}

class AndroidNativeSoundSpecialReader : VfsSpecialReader<NativeSound>(NativeSound::class) {
	suspend override fun readSpecial(vfs: Vfs, path: String): NativeSound = try {
		when (vfs) {
			is LocalVfs -> nativeSoundProvider.createSound(vfs[path])
			else -> nativeSoundProvider.createSound(vfs[path])
		}
	} catch (e: Throwable) {
		e.printStackTrace()
		nativeSoundProvider.createSound(AudioData(44100, 2, shortArrayOf()))
	}
}
