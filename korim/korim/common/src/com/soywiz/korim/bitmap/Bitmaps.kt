package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*

object Bitmaps {
	val transparent = Bitmap32(0, 0)
	val white = Bitmap32(1, 1, IntArray(1) { Colors.WHITE })

	val transparentSlice = transparent.slice()
	val whiteSlice = white.slice()
}
