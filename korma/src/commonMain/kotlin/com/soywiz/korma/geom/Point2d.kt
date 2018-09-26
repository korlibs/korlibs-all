package com.soywiz.korma.geom

import com.soywiz.korma.*

typealias Point2d = Vector2
typealias MPoint2d = MVector2
typealias Point = Vector2
typealias MPoint = MVector2

@Deprecated("", replaceWith = ReplaceWith("Point2d"), level = DeprecationLevel.ERROR)
typealias IPoint2d = Point2d

// @TODO: Check if this avoid boxing!
//inline fun Point2d(x: Number, y: Number) = Vector2(x.toDouble(), y.toDouble())

//inline fun IPoint2d(x: Number, y: Number) = Vector2(x.toDouble(), y.toDouble())
//inline fun Point(x: Number, y: Number) = Point2d(x.toDouble(), y.toDouble())
//inline fun IPoint(x: Number, y: Number) = Vector2(x.toDouble(), y.toDouble())

fun Iterable<Point2d>.getPolylineLength(): Double {
	var out = 0.0
	var prev: Point2d? = null
	for (cur in this) {
		if (prev != null) out += prev.distanceTo(cur)
		prev = cur
	}
	return out
}