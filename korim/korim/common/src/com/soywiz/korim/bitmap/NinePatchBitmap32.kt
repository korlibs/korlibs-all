package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*
import com.soywiz.korio.util.*

class NinePatchBitmap32(val bmp: Bitmap32) {
	val hsegments = (0 until bmp.width).computeRle { RGBA.getA(bmp[it, 0]) != 0 }
	val vsegments = (0 until bmp.height).computeRle { RGBA.getA(bmp[0, it]) != 0 }
}

fun Bitmap.asNinePatch() = NinePatchBitmap32(this.toBMP32())
suspend fun VfsFile.readNinePatch(format: ImageFormat) = NinePatchBitmap32(readBitmap(format).toBMP32())
