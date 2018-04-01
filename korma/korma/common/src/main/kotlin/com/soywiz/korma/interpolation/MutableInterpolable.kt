package com.soywiz.korma.interpolation

interface MutableInterpolable<T> {
	fun setToInterpolated(l: T, r: T, ratio: Double): T
}