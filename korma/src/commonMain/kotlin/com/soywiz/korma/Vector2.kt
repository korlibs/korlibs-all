package com.soywiz.korma

import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import kotlin.math.*

interface Vector2 {
	val x: Double
	val y: Double

	companion object {
		val Zero = Vector2(0, 0)
		val One = Vector2(1, 1)

		val Up = Vector2(0, +1)
		val Down = Vector2(0, -1)
		val Left = Vector2(-1, 0)
		val Right = Vector2(+1, 0)

		operator fun invoke(v: Vector2): Vector2 = Vector2(v.x, v.y)
		inline operator fun invoke(): Vector2 = IVector2(0.0, 0.0)
		inline operator fun invoke(xy: Number): Vector2 = IVector2(xy.toDouble(), xy.toDouble())
		inline operator fun invoke(x: Number, y: Number): Vector2 = IVector2(x.toDouble(), y.toDouble())


		fun middle(a: Vector2, b: Vector2): MVector2 = MVector2((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)

		fun angle(a: Vector2, b: Vector2): Double = acos((a.dot(b)) / (a.length * b.length))

		fun angle(ax: Double, ay: Double, bx: Double, by: Double): Double =
			acos(((ax * bx) + (ay * by)) / (Math.hypot(ax, ay) * Math.hypot(bx, by)))

		fun sortPoints(points: ArrayList<Vector2>): Unit {
			points.sortWith(Comparator { l, r -> cmpPoints(l, r) })
		}

		private fun cmpPoints(l: Vector2, r: Vector2): Int {
			var ret: Double = l.y - r.y
			if (ret == 0.0) ret = l.x - r.x
			if (ret < 0) return -1
			if (ret > 0) return +1
			return 0
		}

		fun angle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Double {
			val ax = x1 - x2
			val ay = y1 - y2
			val al = Math.hypot(ax, ay)

			val bx = x1 - x3
			val by = y1 - y3
			val bl = Math.hypot(bx, by)

			return acos((ax * bx + ay * by) / (al * bl))
		}
	}

	abstract class Base : Vector2 {
		override fun equals(other: Any?): Boolean =
			if (other is Vector2) this.x == other.x && this.y == other.y else false

		override fun hashCode(): Int = x.hashCode() + (y.hashCode() shl 7)
		override fun toString(): String = KormaStr { "(${x.niceStr}, ${y.niceStr})" }
	}
}


@PublishedApi
internal class IVector2(override val x: Double, override val y: Double) : Vector2.Base(), Interpolable<Vector2> {
	override fun interpolateWith(other: Vector2, ratio: Double): Vector2 {
		return Vector2(
			interpolate(this.x, other.x, ratio),
			interpolate(this.y, other.y, ratio)
		)
	}
}

class MVector2(override var x: Double = 0.0, override var y: Double = x) :
	MutableInterpolable<MVector2>, Interpolable<MVector2>, Vector2.Base() {

	constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
	constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
	constructor(x: Long, y: Long) : this(x.toDouble(), y.toDouble())
	constructor(v: Vector2) : this(v.x, v.y)
	//inline constructor(x: Number, y: Number) : this(x.toDouble(), y.toDouble()) // @TODO: Suggest to avoid boxing?

	inline fun setTo(x: Number, y: Number): MVector2 = setTo(x.toDouble(), y.toDouble())

	fun setTo(x: Double, y: Double): MVector2 {
		this.x = x
		this.y = y
		return this
	}

	fun setToZero() = setTo(0.0, 0.0)

	/// Negate this point.
	fun neg() = setTo(-x, -y)

	fun mul(s: Double) = setTo(x * s, y * s)
	fun add(p: Vector2) = this.setToAdd(this, p)
	fun sub(p: Vector2) = this.setToSub(this, p)

	fun copyFrom(that: Vector2) = setTo(that.x, that.y)

	fun setToTransform(mat: IMatrix2d, p: Vector2): MVector2 = setToTransform(mat, p.x, p.y)

	fun setToTransform(mat: IMatrix2d, x: Double, y: Double): MVector2 = setTo(
		mat.transformX(x, y),
		mat.transformY(x, y)
	)

	fun setToAdd(a: Vector2, b: Vector2): MVector2 = setTo(a.x + b.x, a.y + b.y)
	fun setToSub(a: Vector2, b: Vector2): MVector2 = setTo(a.x - b.x, a.y - b.y)

	fun setToMul(a: Vector2, b: Vector2): MVector2 = setTo(a.x * b.x, a.y * b.y)
	fun setToMul(a: Vector2, s: Double): MVector2 = setTo(a.x * s, a.y * s)

	fun setToDiv(a: Vector2, b: Vector2): MVector2 = setTo(a.x / b.x, a.y / b.y)
	fun setToDiv(a: Vector2, s: Double): MVector2 = setTo(a.x / s, a.y / s)

	operator fun plusAssign(that: Vector2) {
		setTo(this.x + that.x, this.y + that.y)
	}

	fun normalize() {
		val len = this.length
		this.setTo(this.x / len, this.y / len)
	}

	val length: Double get() = Math.hypot(x, y)
	/*

	*/

	fun distanceTo(x: Double, y: Double) = Math.hypot(x - this.x, y - this.y)
	fun distanceTo(that: Vector2) = distanceTo(that.x, that.y)

	override fun interpolateWith(other: MVector2, ratio: Double): MVector2 =
		MVector2().setToInterpolated(this, other, ratio)

	override fun setToInterpolated(l: MVector2, r: MVector2, ratio: Double): MVector2 =
		this.setTo(ratio.interpolate(l.x, r.x), ratio.interpolate(l.y, r.y))
}

inline fun Vec(x: Number, y: Number): Vector2 = IVector2(x.toDouble(), y.toDouble())

val MVector2.unit: Vector2 get() = this / length
/*
operator fun MVector2.plus(that: Vector2) = MVector2(this.x + that.x, this.y + that.y)
operator fun MVector2.minus(that: Vector2) = MVector2(this.x - that.x, this.y - that.y)
operator fun MVector2.times(that: Vector2) = this.x * that.x + this.y * that.y
operator fun MVector2.times(v: Double) = MVector2(x * v, y * v)
operator fun MVector2.div(v: Double) = MVector2(x / v, y / v)
*/

// @TODO: mul instead of dot
operator fun Vector2.plus(that: Vector2): Vector2 = Vector2(this.x + that.x, this.y + that.y)

operator fun Vector2.minus(that: Vector2): Vector2 = Vector2(this.x - that.x, this.y - that.y)
operator fun Vector2.times(that: Vector2): Vector2 = Vector2(this.x * that.x, this.y * that.y)
operator fun Vector2.div(that: Vector2): Vector2 = Vector2(this.x / that.x, this.y / that.y)

operator fun Vector2.times(scale: Double): Vector2 = Vector2(this.x * scale, this.y * scale)
operator fun Vector2.div(scale: Double): Vector2 = Vector2(this.x / scale, this.y / scale)

infix fun Vector2.dot(that: Vector2) = this.x * that.x + this.y * that.y
//infix fun Vector2.mul(that: Vector2) = Vector2(this.x * that.x, this.y * that.y)
fun Vector2.distanceTo(x: Double, y: Double) = Math.hypot(x - this.x, y - this.y)

fun Vector2.distanceTo(that: Vector2) = distanceTo(that.x, that.y)

fun Vector2.angleToRad(other: Vector2): Double = Angle.betweenRad(this.x, this.y, other.x, other.y)
fun Vector2.angleTo(other: Vector2): Angle = Angle.between(this.x, this.y, other.x, other.y)

fun Vector2.transformed(mat: IMatrix2d, out: MVector2 = MVector2()): MVector2 = out.setToTransform(mat, this)

operator fun Vector2.get(index: Int) = when (index) {
	0 -> x; 1 -> y
	else -> throw IndexOutOfBoundsException("Vector2 doesn't have $index component")
}

val Vector2.unit: Vector2 get() = this / this.length
val Vector2.length: Double get() = Math.hypot(x, y)
val Vector2.magnitude: Double get() = Math.hypot(x, y)
val Vector2.normalized: Vector2
	get() {
		val imag = 1.0 / magnitude
		return Vector2(x * imag, y * imag)
	}

val Vector2.mutable: MVector2 get() = MVector2(x, y)
val Vector2.immutable: Vector2 get() = Vector2(x, y)
fun Vector2.copy() = Vector2(x, y)

interface Vector2Int {
	val x: Int
	val y: Int

	companion object {
		operator fun invoke(x: Int, y: Int): Vector2Int = IVector2Int(x, y)
	}
}

internal data class IVector2Int(override val x: Int, override val y: Int) : Vector2Int {
	override fun toString(): String = "($x, $y)"
}

data class MVector2Int(override var x: Int = 0, override var y: Int = 0) : Vector2Int {
	fun setTo(x: Int, y: Int) = this.apply { this.x = x; this.y = y }
	fun setTo(that: Vector2Int) = this.setTo(that.x, that.y)
	override fun toString(): String = "($x, $y)"
}

operator fun Vector2Int.plus(that: Vector2Int) = Vector2Int(this.x + that.x, this.y + that.y)
operator fun Vector2Int.minus(that: Vector2Int) = Vector2Int(this.x - that.x, this.y - that.y)
operator fun Vector2Int.times(that: Vector2Int) = Vector2Int(this.x * that.x, this.y * that.y)
operator fun Vector2Int.div(that: Vector2Int) = Vector2Int(this.x / that.x, this.y / that.y)
operator fun Vector2Int.rem(that: Vector2Int) = Vector2Int(this.x % that.x, this.y % that.y)

val Vector2Int.mutable: MVector2Int get() = MVector2Int(x, y)
val Vector2Int.immutable: Vector2Int get() = Vector2Int(x, y)

val Vector2.int get() = Vector2Int(x.toInt(), y.toInt())
val Vector2Int.double get() = Vector2(x.toDouble(), y.toDouble())

@Suppress("NOTHING_TO_INLINE")
class MVector2Area(val size: Int) {
	@PublishedApi internal val points = Array(size) { MPoint2d() }
	@PublishedApi internal var offset = 0
	@PublishedApi internal fun alloc() = points[offset++]

	operator fun Vector2.plus(other: Vector2): Vector2 = alloc().setToAdd(this, other)
	operator fun Vector2.minus(other: Vector2): Vector2 = alloc().setToSub(this, other)

	operator fun Vector2.times(value: Vector2): Vector2 = alloc().setToMul(this, value)
	inline operator fun Vector2.times(value: Number): Vector2 = alloc().setToMul(this, value.toDouble())

	operator fun Vector2.div(value: Vector2): Vector2 = alloc().setToDiv(this, value)
	inline operator fun Vector2.div(value: Number): Vector2 = alloc().setToDiv(this, value.toDouble())

	inline operator fun invoke(callback: MVector2Area.() -> Unit) {
		val oldOffset = offset
		try {
			callback()
		} finally {
			offset = oldOffset
		}
	}
}
