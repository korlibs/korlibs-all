package com.soywiz.korma

import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.Math
import com.soywiz.korma.numeric.niceStr
import kotlin.math.acos

interface IVector2 {
	val x: Double
	val y: Double
}

val IVector2.length: Double get() = Math.hypot(x, y)

inline fun IVector2(x: Number, y: Number) = Vector2.Immutable(x.toDouble(), y.toDouble())

data class Vector2(override var x: Double = 0.0, override var y: Double = x) : MutableInterpolable<Vector2>, Interpolable<Vector2>, IVector2 {
	data class Immutable(override val x: Double, override val y: Double) : IVector2 {
		companion object {
			val ZERO = Immutable(0.0, 0.0)
		}

		fun toMutable() = Vector2(x, y)
	}

	fun toImmutable() = Immutable(x, y)

	constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
	constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
	constructor(x: Long, y: Long) : this(x.toDouble(), y.toDouble())
	constructor(v: IVector2) : this(v.x, v.y)
	//inline constructor(x: Number, y: Number) : this(x.toDouble(), y.toDouble()) // @TODO: Suggest to avoid boxing?

	inline fun setTo(x: Number, y: Number): Vector2 = setTo(x.toDouble(), y.toDouble())

	fun setTo(x: Double, y: Double): Vector2 {
		this.x = x
		this.y = y
		return this
	}

	fun setToZero() = setTo(0.0, 0.0)

	/// Negate this point.
	fun neg() = setTo(-x, -y)

	fun mul(s: Double) = setTo(x * s, y * s)
	fun add(p: IVector2) = this.setToAdd(this, p)
	fun sub(p: IVector2) = this.setToSub(this, p)

	fun copyFrom(that: IVector2) = setTo(that.x, that.y)

	fun setToTransform(mat: IMatrix2d, p: IVector2): Vector2 = setToTransform(mat, p.x, p.y)

	fun setToTransform(mat: IMatrix2d, x: Double, y: Double): Vector2 = setTo(
		mat.transformX(x, y),
		mat.transformY(x, y)
	)

	fun setToAdd(a: IVector2, b: IVector2): Vector2 = setTo(a.x + b.x, a.y + b.y)
	fun setToSub(a: IVector2, b: IVector2): Vector2 = setTo(a.x - b.x, a.y - b.y)
	fun setToMul(a: IVector2, b: IVector2): Vector2 = setTo(a.x * b.x, a.y * b.y)
	fun setToMul(a: IVector2, s: Double): Vector2 = setTo(a.x * s, a.y * s)

	operator fun plusAssign(that: IVector2) {
		setTo(this.x + that.x, this.y + that.y)
	}

	fun normalize() {
		val len = this.length
		this.setTo(this.x / len, this.y / len)
	}

	val unit: Vector2 get() = this / length
	val length: Double get() = Math.hypot(x, y)
	operator fun plus(that: IVector2) = Vector2(this.x + that.x, this.y + that.y)
	operator fun minus(that: IVector2) = Vector2(this.x - that.x, this.y - that.y)
	operator fun times(that: IVector2) = this.x * that.x + this.y * that.y
	operator fun times(v: Double) = Vector2(x * v, y * v)
	operator fun div(v: Double) = Vector2(x / v, y / v)

	fun distanceTo(x: Double, y: Double) = Math.hypot(x - this.x, y - this.y)
	fun distanceTo(that: IVector2) = distanceTo(that.x, that.y)

	override fun toString(): String = "Vector2(${x.niceStr}, ${y.niceStr})"

	override fun interpolateWith(other: Vector2, ratio: Double): Vector2 = Vector2().setToInterpolated(this, other, ratio)
	override fun setToInterpolated(l: Vector2, r: Vector2, ratio: Double): Vector2 = this.setTo(ratio.interpolate(l.x, r.x), ratio.interpolate(l.y, r.y))

	companion object {
		fun middle(a: IVector2, b: IVector2): Vector2 = Vector2((a.x + b.x) * 0.5, (a.y + b.y) * 0.5)

		fun angle(a: IVector2, b: IVector2): Double = acos((a * b) / (a.length * b.length))

		fun angle(ax: Double, ay: Double, bx: Double, by: Double): Double = acos(((ax * bx) + (ay * by)) / (Math.hypot(ax, ay) * Math.hypot(bx, by)))

		fun sortPoints(points: ArrayList<Vector2>): Unit {
			points.sortWith(Comparator({ l, r -> cmpPoints(l, r) }))
		}

		protected fun cmpPoints(l: IVector2, r: IVector2): Int {
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
}

// @TODO: Check if this avoid boxing!
inline fun Vector2(x: Number, y: Number) = Vector2(x.toDouble(), y.toDouble())

operator fun IVector2.times(that: IVector2) = this.x * that.x + this.y * that.y
fun IVector2.distanceTo(x: Double, y: Double) = Math.hypot(x - this.x, y - this.y)
fun IVector2.distanceTo(that: IVector2) = distanceTo(that.x, that.y)
