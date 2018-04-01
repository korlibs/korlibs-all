package com.soywiz.korim

import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.vfs.*

actual object NativeImageSpecialReader {
	actual val instance: VfsSpecialReader<NativeImage> by lazy { AwtImageSpecialReader() }
}