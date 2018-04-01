package com.soywiz.korma.geom

import kotlin.math.*

object Angle {
	fun cos01(ratio: Double) = kotlin.math.cos(PI * 2.0 * ratio)
	fun sin01(ratio: Double) = kotlin.math.sin(PI * 2.0 * ratio)
}