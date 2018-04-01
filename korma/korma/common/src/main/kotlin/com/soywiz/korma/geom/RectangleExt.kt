package com.soywiz.korma.geom

import com.soywiz.korma.math.*

fun Iterable<Rectangle>.bounds(target: Rectangle = Rectangle()): Rectangle {
	var first = true
	var left = 0.0
	var right = 0.0
	var top = 0.0
	var bottom = 0.0
	for (r in this) {
		if (first) {
			left = r.left
			right = r.right
			top = r.top
			bottom = r.bottom
			first = false
		} else {
			left = Math.min(left, r.left)
			right = Math.max(right, r.right)
			top = Math.min(top, r.top)
			bottom = Math.max(bottom, r.bottom)
		}
	}
	return target.setBounds(left, top, right, bottom)
}
