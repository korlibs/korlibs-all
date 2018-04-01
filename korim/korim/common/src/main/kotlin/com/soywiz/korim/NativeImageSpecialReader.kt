package com.soywiz.korim

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.vfs.*

expect object NativeImageSpecialReader {
	val instance: VfsSpecialReader<NativeImage>
}
