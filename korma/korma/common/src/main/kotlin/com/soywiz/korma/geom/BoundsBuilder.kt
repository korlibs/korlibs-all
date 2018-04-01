package com.soywiz.korma.geom

import com.soywiz.korma.Vector2
import com.soywiz.korma.math.Math

class BoundsBuilder {
	val tempRect = Rectangle()

	private var xmin = Double.MAX_VALUE
	private var xmax = Double.MIN_VALUE
	private var ymin = Double.MAX_VALUE
	private var ymax = Double.MIN_VALUE

	fun reset() {
		xmin = Double.MAX_VALUE
		xmax = Double.MIN_VALUE
		ymin = Double.MAX_VALUE
		ymax = Double.MIN_VALUE
	}

	fun add(x: Double, y: Double) = this.apply {
		xmin = Math.min(xmin, x)
		xmax = Math.max(xmax, x)
		ymin = Math.min(ymin, y)
		ymax = Math.max(ymax, y)
		//println("add($x, $y) -> ($xmin,$ymin)-($xmax,$ymax)")
	}

	fun add(p: Vector2) = add(p.x, p.y)

	fun add(ps: Iterable<Vector2>) = this.apply { for (p in ps) add(p) }

	fun add(rect: Rectangle) = this.apply {
		if (rect.isNotEmpty) {
			add(rect.left, rect.top)
			add(rect.right, rect.bottom)
		}
	}

	fun getBounds(out: Rectangle = Rectangle()): Rectangle = out.setBounds(xmin, ymin, xmax, ymax)
}
