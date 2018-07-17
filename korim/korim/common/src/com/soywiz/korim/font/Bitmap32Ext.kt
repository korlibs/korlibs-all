package com.soywiz.korim.font

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*

fun Bitmap32.drawText(font: BitmapFont, str: String, x: Int = 0, y: Int = 0, color: RGBAInt = Colors.WHITE) =
	font.drawText(this, str, x, y, color)
