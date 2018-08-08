package com.soywiz.korim.bitmap

import com.soywiz.kds.*
import com.soywiz.korim.color.*
import com.soywiz.std.*

//var Bitmap.texMipmaps: Boolean by Extra.Property { false }
//fun <T : Bitmap> T.mipmaps(enable: Boolean = true): T = this.apply { this.texMipmaps = enable }

@ThreadLocal // So we can mutate instances with extra properties later
object Bitmaps {
	val transparent = Bitmap32(1, 1).slice(name = "transparent")
	val white = Bitmap32(1, 1, RgbaArray(1) { Colors.WHITE }).slice(name = "white")
}
