package com.soywiz.kmem

import kotlin.math.*

fun Float.reinterpretAsInt() = this.toBits()
fun Int.reinterpretAsFloat() = Float.fromBits(this)

fun Double.reinterpretAsLong() = this.toBits()
fun Long.reinterpretAsDouble() = Double.fromBits(this)

fun Int.rotateLeft(bits: Int): Int = (this shl bits) or (this ushr (32 - bits))

fun Int.rotateRight(bits: Int): Int = (this shl (32 - bits)) or (this ushr bits)

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

fun Int.reverseBits(): Int {
	var v = this
	v = ((v ushr 1) and 0x55555555) or ((v and 0x55555555) shl 1) // swap odd and even bits
	v = ((v ushr 2) and 0x33333333) or ((v and 0x33333333) shl 2) // swap consecutive pairs
	v = ((v ushr 4) and 0x0F0F0F0F) or ((v and 0x0F0F0F0F) shl 4) // swap nibbles ...
	v = ((v ushr 8) and 0x00FF00FF) or ((v and 0x00FF00FF) shl 8) // swap bytes
	v = ((v ushr 16) and 0x0000FFFF) or ((v and 0x0000FFFF) shl 16) // swap 2-byte long pairs
	return v
}

fun Int.countLeadingZeros(): Int {
	var v = this
	if (v == 0) return 32
	var result = 0
	if ((v and 0xFFFF0000.toInt()) == 0) run { v = v shl 16; result += 16; }
	if ((v and 0xFF000000.toInt()) == 0) run { v = v shl 8; result += 8; }
	if ((v and 0xF0000000.toInt()) == 0) run { v = v shl 4; result += 4; }
	if ((v and 0xC0000000.toInt()) == 0) run { v = v shl 2; result += 2; }
	if ((v and 0x80000000.toInt()) == 0) run { v = v shl 1; result += 1; }
	return result
}

fun Int.countTrailingZeros(): Int {
	if (this == 0) return 32
	var n = this
	var c = 32
	n = n and (-n)
	if (n != 0) c--
	if ((n and 0x0000FFFF) != 0) c -= 16
	if ((n and 0x00FF00FF) != 0) c -= 8
	if ((n and 0x0F0F0F0F) != 0) c -= 4
	if ((n and 0x33333333) != 0) c -= 2
	if ((n and 0x55555555) != 0) c -= 1
	return c
}

fun Int.countLeadingOnes(): Int = this.inv().countLeadingZeros()
fun Int.countTrailingOnes(): Int = this.inv().countTrailingZeros()

fun Int.signExtend(bits: Int) = (this shl (32 - bits)) shr (32 - bits)
fun Int.signExtend8(): Int = this shl 24 shr 24
fun Int.signExtend16(): Int = this shl 16 shr 16

fun Long.signExtend(bits: Int): Long = (this shl (64 - bits)) shr (64 - bits)
fun Long.signExtend8(): Long = this shl 24 shr 24
fun Long.signExtend16(): Long = this shl 16 shr 16

fun Byte.toUnsigned() = this.toInt() and 0xFF
fun Int.toUnsigned() = this.toLong() and 0xFFFFFFFFL

inline fun Int.mask(): Int = (1 shl this) - 1
inline fun Long.mask(): Long = (1L shl this.toInt()) - 1L
fun Int.toUInt(): Long = this.toLong() and 0xFFFFFFFFL
fun Int.getBits(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
fun Int.getBit(offset: Int): Boolean = ((this ushr offset) and 1) != 0

fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
fun Int.extract(offset: Int): Boolean = ((this ushr offset) and 1) != 0
fun Int.extract8(offset: Int): Int = (this ushr offset) and 0xFF
fun Int.extract16(offset: Int): Int = (this ushr offset) and 0xFFFF

fun Int.extractSigned(offset: Int, count: Int): Int = ((this ushr offset) and count.mask()).signExtend(count)
fun Int.extract8Signed(offset: Int): Int = (((this ushr offset) and 0xFF) shl 24) shr 24
fun Int.extract16Signed(offset: Int): Int = (((this ushr offset) and 0xFFFF) shl 16) shr 16

fun Int.extractScaled(offset: Int, count: Int, scale: Int): Int = (extract(offset, count) * scale) / count.mask()
fun Int.extractScaledf01(offset: Int, count: Int): Double = extract(offset, count).toDouble() / count.mask().toDouble()

fun Int.extractScaledFF(offset: Int, count: Int): Int = extractScaled(offset, count, 0xFF)
fun Int.extractScaledFFDefault(offset: Int, count: Int, default: Int): Int =
	if (count == 0) default else extractScaled(offset, count, 0xFF)

fun Int.insert(value: Int, offset: Int, count: Int): Int {
	val mask = count.mask()
	val clearValue = this and (mask shl offset).inv()
	return clearValue or ((value and mask) shl offset)
}

fun Int.insert8(value: Int, offset: Int): Int = insert(value, offset, 8)
fun Int.insert(value: Boolean, offset: Int): Int = this.insert(if (value) 1 else 0, offset, 1)
fun Int.insertScaled(value: Int, offset: Int, count: Int, scale: Int): Int =
	insert((value * count.mask()) / scale, offset, count)

fun Int.insertScaledFF(value: Int, offset: Int, count: Int): Int =
	if (count == 0) this else this.insertScaled(value, offset, count, 0xFF)

fun rint(v: Double): Double = if (v >= floor(v) + 0.5) { // @TODO: This is right?
	ceil(v)
} else {
	round(v)
}

fun signum(v: Double): Double = sign(v)

fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this
fun Long.clamp(min: Long, max: Long): Long = if (this < min) min else if (this > max) max else this
fun Double.clamp(min: Double, max: Double): Double = if (this < min) min else if (this > max) max else this
fun Float.clamp(min: Float, max: Float) = when {
	(this < min) -> min
	(this > max) -> max
	else -> this
}

fun Float.isAlmostZero(): Boolean = abs(this) <= 1e-19
fun Float.isNanOrInfinite() = this.isNaN() || this.isInfinite()

//fun toRadians(v: Double): Double = v / 180.0 * 3.141592653589793
//fun toDegrees(v: Double): Double = v * 180.0 / 3.141592653589793

inline infix fun Int.ult(that: Int) = (this xor (-0x80000000)) < (that xor (-0x80000000))
infix fun Int.ule(that: Int) = IntEx.compareUnsigned(this, that) <= 0

infix fun Int.ugt(that: Int) = IntEx.compareUnsigned(this, that) > 0
infix fun Int.uge(that: Int) = IntEx.compareUnsigned(this, that) >= 0

// @TODO: Move to Kmem
fun Int.extractBool(offset: Int) = this.extract(offset, 1) != 0

// @TODO: Move to Kmem
infix fun Int.hasFlag(bits: Int) = (this and bits) == bits

val Int.unsigned: Long get() = this.toLong() and 0xFFFFFFFF

object BitUtils {
	fun mask(value: Int): Int = value.mask()
	fun bitrev32(x: Int): Int = x.reverseBits()
	fun rotr(value: Int, offset: Int): Int = value.rotateRight(offset)
	fun clz32(x: Int): Int = x.countLeadingZeros()
	fun clo(x: Int): Int = clz32(x.inv())
	fun clz(x: Int): Int = clz32(x)
	fun seb(x: Int): Int = (x shl 24) shr 24
	fun seh(x: Int): Int = (x shl 16) shr 16
	fun wsbh(v: Int): Int = ((v and 0xFF00FF00.toInt()) ushr 8) or ((v and 0x00FF00FF) shl 8)
	fun wsbw(v: Int): Int = (
			((v and 0xFF000000.toInt()) ushr 24) or
					((v and 0x00FF0000) ushr 8) or
					((v and 0x0000FF00) shl 8) or
					((v and 0x000000FF) shl 24)
			)
}

fun Int.compareToUnsigned(that: Int) = IntEx.compareUnsigned(this, that)

// l xor MIN_VALUE, r xor MIN_VALUE

//const val INT_MIN_VALUE = -0x80000000
//const val INT_MAX_VALUE = 0x7fffffff


object LongEx {
	val MIN_VALUE: Long = 0x7fffffffffffffffL.inv()
	val MAX_VALUE: Long = 0x7fffffffffffffffL

	fun compare(x: Long, y: Long): Int = if (x < y) -1 else if (x == y) 0 else 1
	fun compareUnsigned(x: Long, y: Long): Int = compare(x xor MIN_VALUE, y xor MIN_VALUE)

	fun divideUnsigned(dividend: Long, divisor: Long): Long {
		if (divisor < 0) return (if (compareUnsigned(dividend, divisor) < 0) 0 else 1).toLong()
		if (dividend >= 0) return dividend / divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return quotient + if (compareUnsigned(rem, divisor) >= 0) 1 else 0
	}

	fun remainderUnsigned(dividend: Long, divisor: Long): Long {
		if (divisor < 0) return if (compareUnsigned(dividend, divisor) < 0) dividend else dividend - divisor
		if (dividend >= 0) return dividend % divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return rem - if (compareUnsigned(rem, divisor) >= 0) divisor else 0
	}
}

object IntEx {
	private val MIN_VALUE = -0x80000000
	private val MAX_VALUE = 0x7fffffff

	fun compare(l: Int, r: Int): Int = if (l < r) -1 else if (l > r) 1 else 0
	fun compareUnsigned(l: Int, r: Int): Int = compare(l xor MIN_VALUE, r xor MIN_VALUE)
	fun divideUnsigned(dividend: Int, divisor: Int): Int {
		if (divisor < 0) return if (compareUnsigned(dividend, divisor) < 0) 0 else 1
		if (dividend >= 0) return dividend / divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return quotient + if (compareUnsigned(rem, divisor) >= 0) 1 else 0
	}

	fun remainderUnsigned(dividend: Int, divisor: Int): Int {
		if (divisor < 0) return if (compareUnsigned(dividend, divisor) < 0) dividend else dividend - divisor
		if (dividend >= 0) return dividend % divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return rem - if (compareUnsigned(rem, divisor) >= 0) divisor else 0
	}
}

infix fun Int.udiv(that: Int) = IntEx.divideUnsigned(this, that)
infix fun Int.urem(that: Int) = IntEx.remainderUnsigned(this, that)

infix fun Long.udiv(that: Long) = LongEx.divideUnsigned(this, that)
infix fun Long.urem(that: Long) = LongEx.remainderUnsigned(this, that)

fun imul32_64(a: Int, b: Int, result: IntArray = IntArray(2)): IntArray {
	if (a == 0) {
		result[0] = 0
		result[1] = 0
		return result
	}
	if (b == 0) {
		result[0] = 0
		result[1] = 0
		return result
	}

	if ((a >= -32768 && a <= 32767) && (b >= -32768 && b <= 32767)) {
		result[0] = a * b
		result[1] = if (result[0] < 0) -1 else 0
		return result
	}

	val doNegate = (a < 0) xor (b < 0)

	umul32_64(abs(a), abs(b), result)

	if (doNegate) {
		result[0] = result[0].inv()
		result[1] = result[1].inv()
		result[0] = (result[0] + 1) or 0
		if (result[0] == 0) result[1] = (result[1] + 1) or 0
	}

	return result
}

fun umul32_64(a: Int, b: Int, result: IntArray = IntArray(2)): IntArray {
	if (a ult 32767 && b ult 65536) {
		result[0] = a * b
		result[1] = if (result[0] < 0) -1 else 0
		return result
	}

	val a00 = a and 0xFFFF
	val a16 = a ushr 16
	val b00 = b and 0xFFFF
	val b16 = b ushr 16
	val c00 = a00 * b00
	var c16 = (c00 ushr 16) + (a16 * b00)
	var c32 = c16 ushr 16
	c16 = (c16 and 0xFFFF) + (a00 * b16)
	c32 += c16 ushr 16
	var c48 = c32 ushr 16
	c32 = (c32 and 0xFFFF) + (a16 * b16)
	c48 += c32 ushr 16

	result[0] = ((c16 and 0xFFFF) shl 16) or (c00 and 0xFFFF)
	result[1] = ((c48 and 0xFFFF) shl 16) or (c32 and 0xFFFF)
	return result
}

fun Double.toIntCeil() = ceil(this).toInt()
fun Double.toIntFloor() = floor(this).toInt()
fun Double.toIntRound() = round(this).toLong().toInt()

val Int.isOdd get() = (this % 2) == 1
val Int.isEven get() = (this % 2) == 0

fun Double.convertRange(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double {
	val ratio = (this - srcMin) / (srcMax - srcMin)
	return (dstMin + (dstMax - dstMin) * ratio)
}

fun Double.convertRangeClamped(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double =
	convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)

fun Long.convertRange(srcMin: Long, srcMax: Long, dstMin: Long, dstMax: Long): Long {
	val ratio = (this - srcMin).toDouble() / (srcMax - srcMin).toDouble()
	return (dstMin + (dstMax - dstMin) * ratio).toLong()
}

fun Int.nextAlignedTo(align: Int) = when {
	align == 0 -> this
	(this % align) == 0 -> this
	else -> (((this / align) + 1) * align)
}

fun Long.nextAlignedTo(align: Long) = when {
	align == 0L -> this
	(this % align) == 0L -> this
	else -> (((this / align) + 1) * align)
}


//fun Int.nextAlignedTo(align: Int) = if (this % align == 0) {
//	this
//} else {
//	(((this / align) + 1) * align)
//}

fun Int.isAlignedTo(alignment: Int) = (this % alignment) == 0


fun Long.toIntSafe(): Int {
	if (this.toInt().toLong() != this) throw IllegalArgumentException("Long doesn't fit Integer")
	return this.toInt()
}

fun Long.toIntClamp(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int {
	if (this < min) return min
	if (this > max) return max
	return this.toInt()
}

fun Long.toUintClamp(min: Int = 0, max: Int = Int.MAX_VALUE) = this.toIntClamp(0, Int.MAX_VALUE)

infix fun Byte.and(mask: Long): Long = this.toLong() and mask

infix fun Byte.and(mask: Int): Int = this.toInt() and mask
infix fun Short.and(mask: Int): Int = this.toInt() and mask

infix fun Byte.or(mask: Int): Int = this.toInt() or mask
infix fun Short.or(mask: Int): Int = this.toInt() or mask
infix fun Short.or(mask: Short): Int = this.toInt() or mask.toInt()

infix fun Byte.shl(that: Int): Int = this.toInt() shl that
infix fun Short.shl(that: Int): Int = this.toInt() shl that

infix fun Byte.shr(that: Int): Int = this.toInt() shr that
infix fun Short.shr(that: Int): Int = this.toInt() shr that

infix fun Byte.ushr(that: Int): Int = this.toInt() ushr that
infix fun Short.ushr(that: Int): Int = this.toInt() ushr that

inline fun Boolean.toInt() = if (this) 1 else 0

val Float.niceStr: String get() = if (this.toLong().toFloat() == this) "${this.toLong()}" else "$this"
val Double.niceStr: String get() = if (this.toLong().toDouble() == this) "${this.toLong()}" else "$this"

val Int.nextPowerOfTwo: Int get() {
	var v = this
	v--
	v = v or (v shr 1)
	v = v or (v shr 2)
	v = v or (v shr 4)
	v = v or (v shr 8)
	v = v or (v shr 16)
	v++
	return v
}
val Int.isPowerOfTwo: Boolean get() = this.nextPowerOfTwo == this
