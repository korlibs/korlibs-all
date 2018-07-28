package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*

object Bitmaps {
	val transparent = Bitmap32(1, 1).slice(name = "transparent")
	val white = Bitmap32(1, 1, RgbaArray(1) { Colors.WHITE }).slice(name = "white")
}
