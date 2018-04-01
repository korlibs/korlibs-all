package com.soywiz.korau

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.UrlVfs
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsSpecialReader

class HtmlNativeSoundSpecialReader : VfsSpecialReader<NativeSound>(NativeSound::class) {
	suspend override fun readSpecial(vfs: Vfs, path: String): NativeSound = when (vfs) {
		is LocalVfs -> {
			//println("LOCAL: HtmlNativeSoundSpecialReader: $vfs, $path")
			HtmlNativeSound(path)
		}
		is UrlVfs -> {
			//println("URL: HtmlNativeSoundSpecialReader: $vfs, $path")
			HtmlNativeSound(vfs.getFullUrl(path))
		}
		else -> {
			//println("OTHER: HtmlNativeSoundSpecialReader: $vfs, $path")
			HtmlNativeSoundProviderImpl.createSound(vfs[path].readBytes())
		}
	}
}