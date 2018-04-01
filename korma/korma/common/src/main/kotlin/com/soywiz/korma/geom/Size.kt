package com.soywiz.korma.geom

import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.Math
import com.soywiz.korma.numeric.niceStr

interface ISize {
	val width: Double
	val height: Double
}

data class Size(override var width: Double, override var height: Double) : MutableInterpolable<Size>, Interpolable<Size>, Sizeable, ISize {
	data class Immutable(override val width: Double, override val height: Double) : ISize

	override val size: Size = this

	fun setTo(width: Double, height: Double) = this.apply {
		this.width = width
		this.height = height
	}

	val area: Double get() = width * height
	val perimeter: Double get() = width * 2 + height * 2
	val min: Double get() = Math.min(width, height)
	val max: Double get() = Math.max(width, height)

	fun clone() = Size(width, height)

	override fun interpolateWith(other: Size, ratio: Double): Size = Size(0, 0).setToInterpolated(this, other, ratio)

	override fun setToInterpolated(l: Size, r: Size, ratio: Double): Size = this.setTo(
		ratio.interpolate(l.width, r.width),
		ratio.interpolate(l.height, r.height)
	)

	override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

inline fun Size(width: Number, height: Number) = Size(width.toDouble(), height.toDouble())
inline fun ISize(width: Number, height: Number) = Size.Immutable(width.toDouble(), height.toDouble())

interface Sizeable {
	val size: Size
}