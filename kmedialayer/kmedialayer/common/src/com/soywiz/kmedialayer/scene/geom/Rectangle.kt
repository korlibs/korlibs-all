package com.soywiz.kmedialayer.scene.geom

data class Rectangle(
	var x: Double = 0.0,
	var y: Double = 0.0,
	var width: Double = 0.0,
	var height: Double = 0.0
) {
	val left get() = x
	val top get() = y
	val right get() = x + width
	val bottom get() = y + height

	fun setTo(x: Double, y: Double, width: Double, height: Double) {
		this.x = x; this.y = y
		this.width = width; this.height = height
	}

	inline fun setTo(x: Number, y: Number, width: Number, height: Number) =
		setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
}