package com.soywiz.korma.math

import kotlin.math.*

fun Float.isAlmostZero(): Boolean = abs(this) <= 1e-19
fun Float.clamp(min: Float, max: Float) = when {
	(this < min) -> min
	(this > max) -> max
	else -> this
}
fun Float.isNanOrInfinite() = this.isNaN() || this.isInfinite()

fun Float.reinterpretAsInt() = this.toBits()
fun Int.reinterpretAsFloat() = Float.fromBits(this)

fun Double.reinterpretAsLong() = this.toBits()
fun Long.reinterpretAsDouble() = Double.fromBits(this)

fun Int.reverseBytes(): Int {
	val v0 = ((this ushr 0) and 0xFF)
	val v1 = ((this ushr 8) and 0xFF)
	val v2 = ((this ushr 16) and 0xFF)
	val v3 = ((this ushr 24) and 0xFF)
	return (v0 shl 24) or (v1 shl 16) or (v2 shl 8) or (v3 shl 0)
}

fun Short.reverseBytes(): Short {
	val low = ((this.toInt() ushr 0) and 0xFF)
	val high = ((this.toInt() ushr 8) and 0xFF)
	return ((high and 0xFF) or (low shl 8)).toShort()
}

fun Char.reverseBytes(): Char {
	val low = ((this.toInt() ushr 0) and 0xFF)
	val high = ((this.toInt() ushr 8) and 0xFF)
	return ((high and 0xFF) or (low shl 8)).toChar()
}

fun Long.reverseBytes(): Long {
	val v0 = (this ushr 0).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
	val v1 = (this ushr 32).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
	return (v0 shl 32) or (v1 shl 0)
}

fun rint(v: Double): Double = Math.rintDouble(v)

fun toRadians(v: Double): Double = v / 180.0 * 3.141592653589793
fun toDegrees(v: Double): Double = v * 180.0 / 3.141592653589793
fun signum(v: Double): Double = sign(v)

fun clamp(v: Int, min: Int, max: Int): Int = if (v < min) min else if (v > max) max else v
fun clamp(v: Long, min: Long, max: Long): Long = if (v < min) min else if (v > max) max else v
fun clamp(v: Double, min: Double, max: Double): Double = if (v < min) min else if (v > max) max else v
