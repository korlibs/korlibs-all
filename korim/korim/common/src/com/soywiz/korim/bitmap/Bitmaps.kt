package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*

object Bitmaps {
	val transparentBitmap = Bitmap32(0, 0)
	val whiteBitmap = Bitmap32(1, 1, IntArray(1) { Colors.WHITE })

	val transparent = transparentBitmap.slice()
	val white = whiteBitmap.slice()
}
