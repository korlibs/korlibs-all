package com.soywiz.korma.geom

import com.soywiz.korma.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*

interface IRectangle {
	val x: Double
	val y: Double
	val width: Double
	val height: Double
}

data class Rectangle(
	override var x: Double = 0.0, override var y: Double = 0.0,
	override var width: Double = 0.0, override var height: Double = 0.0
) : MutableInterpolable<Rectangle>, Interpolable<Rectangle>, IRectangle, Sizeable {
	data class Immutable(
		override val x: Double,
		override val y: Double,
		override val width: Double,
		override val height: Double
	) : IRectangle {
		fun toMutable() = Rectangle(x, y, width, height)
	}

	fun toImmutable() = Immutable(x, y, width, height)

	constructor(x: Int, y: Int, width: Int, height: Int) : this(
		x.toDouble(),
		y.toDouble(),
		width.toDouble(),
		height.toDouble()
	)

	val isEmpty: Boolean get() = area == 0.0
	val isNotEmpty: Boolean get() = area != 0.0
	val area: Double get() = width * height
	var left: Double; get() = x; set(value) = run { x = value }
	var top: Double; get() = y; set(value) = run { y = value }
	var right: Double; get() = x + width; set(value) = run { width = value - x }
	var bottom: Double; get() = y + height; set(value) = run { height = value - y }

	override val size: Size get() = Size(width, height)

	inline fun setTo(x: Number, y: Number, width: Number, height: Number) =
		this.setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

	fun setTo(x: Double, y: Double, width: Double, height: Double) = this.apply {
		this.x = x
		this.y = y
		this.width = width
		this.height = height
	}

	fun copyFrom(that: Rectangle) = setTo(that.x, that.y, that.width, that.height)

	fun setBounds(left: Double, top: Double, right: Double, bottom: Double) =
		setTo(left, top, right - left, bottom - top)

	fun setBounds(left: Int, top: Int, right: Int, bottom: Int) = setTo(left, top, right - left, bottom - top)

	operator fun times(scale: Double) = Rectangle(x * scale, y * scale, width * scale, height * scale)
	operator fun div(scale: Double) = Rectangle(x / scale, y / scale, width / scale, height / scale)

	operator fun contains(that: Rectangle) = isContainedIn(that, this)
	operator fun contains(that: Point2d) = contains(x, y)
	fun contains(x: Double, y: Double) = (x >= left && x < right) && (y >= top && y < bottom)

	infix fun intersects(that: Rectangle): Boolean = intersectsX(that) && intersectsY(that)

	infix fun intersectsX(that: Rectangle): Boolean = that.left <= this.right && that.right >= this.left
	infix fun intersectsY(that: Rectangle): Boolean = that.top <= this.bottom && that.bottom >= this.top

	fun setToIntersection(a: Rectangle, b: Rectangle) = this.apply { a.intersection(b, this) }

	infix fun intersection(that: Rectangle) = intersection(that, Rectangle())

	fun intersection(that: Rectangle, target: Rectangle = Rectangle()) = if (this intersects that) target.setBounds(
		Math.max(this.left, that.left), Math.max(this.top, that.top),
		Math.min(this.right, that.right), Math.min(this.bottom, that.bottom)
	) else null

	fun displaced(dx: Double, dy: Double) = Rectangle(this.x + dx, this.y + dy, width, height)
	fun displace(dx: Double, dy: Double) = setTo(this.x + dx, this.y + dy, this.width, this.height)

	fun inflate(dx: Double, dy: Double) {
		x -= dx; width += 2 * dx
		y -= dy; height += 2 * dy
	}

	fun clone() = Rectangle(x, y, width, height)

	fun setToAnchoredRectangle(small: Rectangle, anchor: Anchor, big: Rectangle) = setTo(
		anchor.sx * (big.width - small.width),
		anchor.sy * (big.height - small.height),
		small.width,
		small.height
	)

	//override fun toString(): String = "Rectangle([${left.niceStr}, ${top.niceStr}]-[${right.niceStr}, ${bottom.niceStr}])"
	override fun toString(): String = KormaStr {
		"Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"
	}

	fun toStringBounds(): String =
		KormaStr { "Rectangle([${left.niceStr},${top.niceStr}]-[${right.niceStr},${bottom.niceStr}])" }

	companion object {
		fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): Rectangle =
			Rectangle().setBounds(left, top, right, bottom)

		fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): Rectangle =
			Rectangle().setBounds(left, top, right, bottom)

		fun isContainedIn(a: Rectangle, b: Rectangle): Boolean =
			a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
	}

	override fun interpolateWith(other: Rectangle, ratio: Double): Rectangle =
		Rectangle().setToInterpolated(this, other, ratio)

	override fun setToInterpolated(l: Rectangle, r: Rectangle, ratio: Double): Rectangle = this.setTo(
		ratio.interpolate(l.x, r.x),
		ratio.interpolate(l.y, r.y),
		ratio.interpolate(l.width, r.width),
		ratio.interpolate(l.height, r.height)
	)

	fun getAnchoredPosition(anchor: Anchor, out: MVector2 = MVector2()): MVector2 =
		out.setTo(left + width * anchor.sx, top + height * anchor.sy)

	fun toInt() = RectangleInt(x, y, width, height)
}

// @TODO: Check if this avoid boxing!
inline fun Rectangle(x: Number, y: Number, width: Number, height: Number) =
	Rectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

inline fun IRectangle(x: Number, y: Number, width: Number, height: Number) =
	Rectangle.Immutable(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
