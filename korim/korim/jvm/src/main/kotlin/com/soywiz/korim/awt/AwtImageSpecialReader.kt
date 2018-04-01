package com.soywiz.korim.awt

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.vfs.*
import java.io.*

class AwtImageSpecialReader : VfsSpecialReader<NativeImage>(NativeImage::class) {
	override suspend fun readSpecial(vfs: Vfs, path: String): NativeImage {
		return when (vfs) {
			is LocalVfs -> {
				//println("LOCAL: AwtImageSpecialReader.readSpecial: $vfs, $path")
				AwtNativeImage(awtReadImageInWorker(File(path)))
			}
			else -> {
				//println("OTHER: AwtImageSpecialReader.readSpecial: $vfs, $path")
				AwtNativeImage(awtReadImageInWorker(vfs[path].readAll()))
			}
		}
	}
}