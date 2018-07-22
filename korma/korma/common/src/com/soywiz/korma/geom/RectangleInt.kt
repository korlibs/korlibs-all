package com.soywiz.korma.geom

import com.soywiz.korma.*

interface IRectangleInt {
	val x: Int
	val y: Int
	val width: Int
	val height: Int
}

data class RectangleInt(val position: MPositionInt, val size: SizeInt) : IRectangleInt {
	constructor(x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0) : this(
		MPositionInt(x, y),
		SizeInt(width, height)
	)

	companion object {
	    fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): RectangleInt = RectangleInt(left, top, right - left, bottom - top)
	}

	data class Immutable(override val x: Int, override val y: Int, override val width: Int, override val height: Int) :
		IRectangleInt {
		fun toMutable() = RectangleInt(x, y, width, height)
	}

	fun toImmutable() = Immutable(x, y, width, height)

	override var x: Int set(value) = run { position.x = value }; get() = position.x
	override var y: Int set(value) = run { position.y = value }; get() = position.y

	override var width: Int set(value) = run { size.width = value }; get() = size.width
	override var height: Int set(value) = run { size.height = value }; get() = size.height

	var left: Int set(value) = run { x = value }; get() = x
	var top: Int set(value) = run { y = value }; get() = y

	var right: Int set(value) = run { width = value - x }; get() = x + width
	var bottom: Int set(value) = run { height = value - y }; get() = y + height

	fun setTo(that: RectangleInt) = setTo(that.x, that.y, that.width, that.height)

	fun setTo(x: Int, y: Int, width: Int, height: Int) = this.apply {
		this.x = x
		this.y = y
		this.width = width
		this.height = height
	}

	fun setPosition(x: Int, y: Int) = this.apply { this.position.setTo(x, y) }

	fun setSize(width: Int, height: Int) = this.apply {
		this.size.setTo(width, height)
		this.width = width
		this.height = height
	}

	fun setBoundsTo(left: Int, top: Int, right: Int, bottom: Int) = setTo(left, top, right - left, bottom - top)

	fun anchoredIn(container: RectangleInt, anchor: Anchor, out: RectangleInt = RectangleInt()): RectangleInt =
		out.setTo(
			((container.width - this.width) * anchor.sx).toInt(),
			((container.height - this.height) * anchor.sy).toInt(),
			width,
			height
		)

	fun getAnchorPosition(anchor: Anchor, out: MPositionInt = MPositionInt()): MPositionInt =
		out.setTo((x + width * anchor.sx).toInt(), (y + height * anchor.sy).toInt())

	operator fun contains(v: SizeInt): Boolean = (v.width <= width) && (v.height <= height)

	fun toDouble() = Rectangle(x, y, width, height)

	override fun toString(): String = "IRectangle(x=$x, y=$y, width=$width, height=$height)"
}

val IRectangle.int get() = RectangleInt(x, y, width, height)
val IRectangleInt.double get() = Rectangle(x, y, width, height)

fun IRectangleInt.anchor(ax: Double, ay: Double): Vector2Int =
	PointInt((x + width * ax).toInt(), (y + height * ay).toInt())

val IRectangleInt.center get() = anchor(0.5, 0.5)

inline fun RectangleInt(x: Number, y: Number, width: Number, height: Number) =
	RectangleInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())

inline fun IRectangleInt(x: Number, y: Number, width: Number, height: Number) =
	RectangleInt.Immutable(x.toInt(), y.toInt(), width.toInt(), height.toInt())
