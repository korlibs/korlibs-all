package com.soywiz.korau

import com.soywiz.korau.sound.*
import com.soywiz.korio.vfs.*

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