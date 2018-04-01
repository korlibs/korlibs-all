package com.soywiz.korma.geom

interface IPointInt {
	val x: Int
	val y: Int
}

data class PointInt(override var x: Int = 0, override var y: Int = x) : IPointInt {
	data class Immutable(override val x: Int, override val y: Int) : IPointInt

	fun setTo(x: Int, y: Int) = this.apply { this.x = x; this.y = y }
	fun setTo(that: PointInt) = this.setTo(that.x, that.y)

	operator fun plus(that: IPointInt) = PointInt(this.x + that.x, this.y + that.y)
	operator fun minus(that: IPointInt) = PointInt(this.x - that.x, this.y - that.y)
}

inline fun PointInt(x: Number, y: Number) = PointInt(x.toInt(), y.toInt())
inline fun IPointInt(x: Number, y: Number) = PointInt.Immutable(x.toInt(), y.toInt())

operator fun IPointInt.plus(that: IPointInt) = PointInt.Immutable(this.x + that.x, this.y + that.y)
operator fun IPointInt.minus(that: IPointInt) = PointInt.Immutable(this.x - that.x, this.y - that.y)
