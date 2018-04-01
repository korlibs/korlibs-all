package com.soywiz.korge.render

import com.soywiz.korim.bitmap.*
import com.soywiz.korma.numeric.*

fun Bitmap32.ensurePowerOfTwo(): Bitmap32 {
	if (this.width.isPowerOfTwo && this.height.isPowerOfTwo) {
		return this
	} else {
		val out = Bitmap32(this.width.nextPowerOfTwo, this.height.nextPowerOfTwo)
		out.put(this)
		return out
	}
}
