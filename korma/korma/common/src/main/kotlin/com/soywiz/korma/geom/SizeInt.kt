package com.soywiz.korma.geom

data class SizeInt(var width: Int = 0, var height: Int = width) {
	operator fun contains(v: SizeInt): Boolean = (v.width <= width) && (v.height <= height)

	operator fun times(v: Double) = SizeInt((width * v).toInt(), (height * v).toInt())

	fun setTo(width: Int, height: Int) = this.apply {
		this.width = width
		this.height = height
	}

	fun setTo(that: SizeInt) = setTo(that.width, that.height)

	fun applyScaleMode(container: SizeInt, mode: ScaleMode, out: SizeInt = SizeInt()): SizeInt = mode(this, container, out)

	fun fitTo(container: SizeInt, out: SizeInt = SizeInt()): SizeInt = applyScaleMode(container, ScaleMode.SHOW_ALL, out)

	fun setToScaled(sx: Double, sy: Double = sx) = setTo((this.width * sx).toInt(), (this.height * sy).toInt())

	fun anchoredIn(container: RectangleInt, anchor: Anchor, out: RectangleInt = RectangleInt()): RectangleInt {
		return out.setTo(((container.width - this.width) * anchor.sx).toInt(), ((container.height - this.height) * anchor.sy).toInt(), width, height)
	}

	fun getAnchorPosition(anchor: Anchor, out: PositionInt = PositionInt()): PositionInt = out.setTo((width * anchor.sx).toInt(), (height * anchor.sy).toInt())
}