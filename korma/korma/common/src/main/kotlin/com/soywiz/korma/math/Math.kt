package com.soywiz.korma.math

import com.soywiz.korma.Vector2
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.interpolation.Interpolable
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.pow

object Math {
	@Deprecated("", ReplaceWith("kotlin.math.round(v)", "kotlin"))
	fun round(v: Double): Double = kotlin.math.round(v)

	@Deprecated("", ReplaceWith("kotlin.math.cos(value)", "kotlin"))
	fun cos(value: Double): Double = kotlin.math.cos(value)

	@Deprecated("", ReplaceWith("kotlin.math.sin(value)", "kotlin"))
	fun sin(value: Double): Double = kotlin.math.sin(value)

	@Deprecated("", ReplaceWith("kotlin.math.tan(value)", "kotlin"))
	fun tan(value: Double): Double = kotlin.math.tan(value)

	@Deprecated("", ReplaceWith("kotlin.math.sqrt(value)", "kotlin"))
	fun sqrt(value: Double): Double = kotlin.math.sqrt(value)

	@Deprecated("", ReplaceWith("kotlin.math.acos(value)", "kotlin"))
	fun acos(value: Double): Double = kotlin.math.acos(value)

	@Deprecated("", ReplaceWith("kotlin.math.atan(value)", "kotlin"))
	fun atan(value: Double): Double = kotlin.math.atan(value)

	@Deprecated("", ReplaceWith("kotlin.math.atan2(y, x)", "kotlin"))
	fun atan2(y: Double, x: Double): Double = kotlin.math.atan2(y, x)

	@Deprecated("", ReplaceWith("kotlin.math.hypot(a, b)", "kotlin"))
	fun len(a: Double, b: Double) = kotlin.math.hypot(a, b)

	@Deprecated("", ReplaceWith("Float.fromBits(value)"))
	fun reinterpretIntFloat(value: Int): Float = Float.fromBits(value)

	fun interpolate(min: Int, max: Int, ratio: Double): Int = min + ((max - min) * ratio).toInt()
	fun interpolate(min: Long, max: Long, ratio: Double) = min + ((max - min) * ratio).toLong()

	fun <T : Interpolable<T>> interpolate(min: T, max: T, ratio: Double): T = min.interpolateWith(max, ratio)

	fun interpolateAny(min: Any, max: Any, ratio: Double): Any {
		return when (min) {
			is Int -> interpolate(min, max as Int, ratio)
			is Long -> interpolate(min, max as Long, ratio)
			is Double -> interpolate(min, max as Double, ratio)
			is Vector2 -> min.setToInterpolated(min, max as Vector2, ratio)
			else -> throw RuntimeException("Unsupported interpolate with $min")
		}
	}

	fun min(a: Int, b: Int): Int = kotlin.math.min(a, b)
	fun max(a: Int, b: Int): Int = kotlin.math.max(a, b)

	fun min(a: Double, b: Double): Double = kotlin.math.min(a, b)
	fun max(a: Double, b: Double): Double = kotlin.math.max(a, b)

	fun min(a: Double, b: Double, c: Double, d: Double): Double = min(min(a, b), min(c, d))
	fun max(a: Double, b: Double, c: Double, d: Double): Double = max(max(a, b), max(c, d))

	fun clamp(v: Long, min: Long, max: Long): Long = if (v < min) min else if (v > max) max else v
	fun clamp(v: Int, min: Int, max: Int): Int = if (v < min) min else if (v > max) max else v
	fun clamp(value: Double, min: Double, max: Double): Double = if (value < min) min else if (value > max) max else value
	fun clampSpecial(value: Double, min: Double, max: Double): Double = if (max >= min) clamp(value, min, max) else value

	fun clamp(value: Float, min: Float, max: Float): Float = if (value < min) min else if (value > max) max else value
	fun clampInt(value: Int, min: Int, max: Int): Int = if (value < min) min else if (value > max) max else value
	fun clampf255(v: Double): Int = if (v < 0.0) 0 else if (v > 1.0) 255 else (v * 255).toInt()
	fun clampf01(v: Double): Double = if (v < 0.0) 0.0 else if (v > 1.0) 1.0 else v
	fun clampn255(v: Int): Int = if (v < -255) -255 else if (v > 255) 255 else v
	fun clamp255(v: Int): Int = if (v < 0) 0 else if (v > 255) 255 else v

	fun distance(a: Double, b: Double): Double = kotlin.math.abs(a - b)
	fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = Math.hypot(x1 - x2, y1 - y2)
	fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double = Math.hypot((x1 - x2).toDouble(), (y1 - y2).toDouble())
	fun distance(a: Vector2, b: Vector2): Double = distance(a.x, a.y, b.x, b.y)
	fun distance(a: PointInt, b: PointInt): Double = distance(a.x, a.y, b.x, b.y)

	fun smoothstep(edge0: Double, edge1: Double, step: Double): Double {
		val step2 = clamp((step - edge0) / (edge1 - edge0), 0.0, 1.0)
		return step2 * step2 * (3 - 2 * step2)
	}

	fun interpolate(v0: Double, v1: Double, step: Double): Double = v0 * (1 - step) + v1 * step

	fun modUnsigned(num: Double, den: Double): Double {
		var result: Double = (num % den)
		if (result < 0) result += den
		return result
	}

	fun between(value: Double, min: Double, max: Double): Boolean = (value >= min) && (value <= max)

	fun convertRange(value: Double, minSrc: Double, maxSrc: Double, minDst: Double, maxDst: Double): Double = (((value - minSrc) / (maxSrc - minSrc)) * (maxDst - minDst)) + minDst

	fun sign(x: Double): Int = if (x < 0) -1 else if (x > 0) +1 else 0
	fun signNonZeroM1(x: Double): Int = if (x <= 0) -1 else +1
	fun signNonZeroP1(x: Double): Int = if (x >= 0) +1 else -1

	fun multiplyIntegerUnsigned(a: Int, b: Int) = (a * b) or 0
	fun multiplyIntegerSigned(a: Int, b: Int): Int = (a * b) or 0
	fun divideIntegerUnsigned(a: Int, b: Int): Int = (a / b) or 0
	fun divideIntegerSigned(a: Int, b: Int): Int = (a / b) or 0
	fun hypot(x: Double, y: Double): Double = kotlin.math.sqrt(x * x + y * y)
	fun hypotNoSqrt(x: Double, y: Double): Double = (x * x + y * y)

	fun roundDecimalPlaces(value: Double, places: Int): Double {
		val placesFactor: Double = 10.0.pow(places.toDouble())
		return kotlin.math.round(value * placesFactor) / placesFactor
	}

	fun isEquivalent(a: Double, b: Double, epsilon: Double = 0.0001): Boolean = (a - epsilon < b) && (a + epsilon > b)
	fun packUintFast(r: Int, g: Int, b: Int, a: Int): Int = (a shl 24) or (b shl 16) or (g shl 8) or (r shl 0)
	fun pack4fUint(r: Double, g: Double, b: Double, a: Double): Int = packUintFast(clampf255(r), clampf255(g), clampf255(b), clampf255(a))
	fun log2(v: Int): Int = log(v.toDouble(), base = 2.0).toInt()

	fun distanceXY(x1: Double, y1: Double, x2: Double, y2: Double): Double = hypot(x1 - x2, y1 - y2);
	fun distancePoint(a: Vector2, b: Vector2): Double = distanceXY(a.x, a.y, b.x, b.y);

	@Deprecated("", ReplaceWith("kotlin.math.abs(v)", "kotlin"))
	fun abs(v: Int): Int = kotlin.math.abs(v)

	@Deprecated("", ReplaceWith("kotlin.math.abs(v)", "kotlin"))
	fun abs(v: Long): Long = kotlin.math.abs(v)

	@Deprecated("", ReplaceWith("kotlin.math.abs(v)", "kotlin"))
	fun abs(v: Float): Float = kotlin.math.abs(v)

	@Deprecated("", ReplaceWith("kotlin.math.abs(v)", "kotlin"))
	fun abs(v: Double): Double = kotlin.math.abs(v)

	fun handleCastInfinite(value: Float): Int {
		return if (value < 0) -2147483648 else 2147483647
	}

	fun rintDouble(value: Double): Double {
		val twoToThe52 = 2.0.pow(52); // 2^52
		val sign = kotlin.math.sign(value); // preserve sign info
		var rvalue = kotlin.math.abs(value);
		if (rvalue < twoToThe52) rvalue = ((twoToThe52 + rvalue) - twoToThe52);
		return sign * rvalue; // restore original sign
	}

	fun rint(value: Float): Int {
		if (value.isNanOrInfinite()) return handleCastInfinite(value)
		return rintDouble(value.toDouble()).toInt()
	}

	fun cast(value: Float): Int {
		if (value.isNanOrInfinite()) return handleCastInfinite(value)
		return if (value < 0) kotlin.math.ceil(value).toInt() else kotlin.math.floor(value).toInt()
	}

	fun trunc(value: Float): Int {
		if (value.isNanOrInfinite()) return handleCastInfinite(value)
		if (value < 0) {
			return kotlin.math.ceil(value).toInt()
		} else {
			return kotlin.math.floor(value).toInt()
		}
	}

	fun round(value: Float): Int {
		if (value.isNanOrInfinite()) return handleCastInfinite(value)
		return kotlin.math.round(value).toInt()
	}

	fun floor(value: Float): Int {
		if (value.isNanOrInfinite()) return handleCastInfinite(value)
		return kotlin.math.floor(value).toInt()
	}

	fun ceil(value: Float): Int {
		if (value.isNanOrInfinite()) return handleCastInfinite(value)
		return kotlin.math.ceil(value).toInt()
	}
}
