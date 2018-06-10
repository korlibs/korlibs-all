package com.soywiz.korma.interpolation

interface Interpolable<T> {
	fun interpolateWith(other: T, ratio: Double): T
}