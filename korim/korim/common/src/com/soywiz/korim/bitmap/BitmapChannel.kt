package com.soywiz.korim.bitmap

import com.soywiz.korim.color.*

enum class BitmapChannel(val index: Int) {
	RED(0), GREEN(1), BLUE(2), ALPHA(3);

	val shift = index * 8
	val setMask = (0xFF shl shift)
	val clearMask = setMask.inv()

	fun extract(rgba: RGBA): Int = (rgba.rgba ushr shift) and 0xFF
	fun insert(rgba: RGBA, value: Int): RGBA = RGBA((rgba.rgba and clearMask) or ((value and 0xFF) shl shift))

	fun extractInt(rgba: Int): Int = (rgba ushr shift) and 0xFF
	fun insertInt(rgba: Int, value: Int): Int = RGBAInt((rgba and clearMask) or ((value and 0xFF) shl shift))

	companion object {
		val ALL = values()
		operator fun get(index: Int) = ALL[index]
	}
}

val BitmapChannel.Companion.Y get() = BitmapChannel.RED
val BitmapChannel.Companion.Cb get() = BitmapChannel.GREEN
val BitmapChannel.Companion.Cr get() = BitmapChannel.BLUE
val BitmapChannel.Companion.A get() = BitmapChannel.ALPHA

fun BitmapChannel.toStringYCbCr() = when (this.index) {
	0 -> "Y"
	1 -> "Cb"
	2 -> "Cr"
	3 -> "A"
	else -> com.soywiz.korio.error.invalidOp
}
