package com.soywiz.kds

import kotlin.math.*

object KdsExt {
	infix fun Int.divCeil(that: Int): Int = if (this % that != 0) (this / that) + 1 else (this / that)

	infix fun Int.umod(other: Int): Int {
		val remainder = this % other
		return when {
			remainder < 0 -> remainder + other
			else -> remainder
		}
	}

	infix fun Double.umod(other: Double): Double {
		val remainder = this % other
		return when {
			remainder < 0 -> remainder + other
			else -> remainder
		}
	}

	// @TODO: Use bit counting instead
	fun ilog2(v: Int): Int = log2(v.toDouble()).toInt()
}

inline fun <T> KdsExt(callback: KdsExt.() -> T) = callback(KdsExt)
