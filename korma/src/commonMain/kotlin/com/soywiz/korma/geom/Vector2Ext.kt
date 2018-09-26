package com.soywiz.korma.geom

import com.soywiz.korma.*

fun Iterable<Vector2>.bounds(out: Rectangle = Rectangle()): Rectangle = out.setBounds(
	left = this.map { it.x }.min() ?: 0.0,
	top = this.map { it.y }.min() ?: 0.0,
	right = this.map { it.x }.max() ?: 0.0,
	bottom = this.map { it.y }.max() ?: 0.0
)
