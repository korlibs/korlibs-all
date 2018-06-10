package com.soywiz.korma.interpolation

fun interpolate(v0: Int, v1: Int, step: Double): Int = (v0 * (1 - step) + v1 * step).toInt()
fun interpolate(v0: Long, v1: Long, step: Double): Long = (v0 * (1 - step) + v1 * step).toLong()
fun interpolate(v0: Double, v1: Double, step: Double): Double = v0 * (1 - step) + v1 * step

@Suppress("UNCHECKED_CAST", "USELESS_CAST")
fun <T> interpolateAny(min: T, max: T, ratio: Double): T = when (min) {
	is Int -> ratio.interpolate(min as Int, max as Int) as T
	is Long -> ratio.interpolate(min as Long, max as Long) as T
	is Float -> ratio.interpolate(min as Float, max as Float) as T
	is Double -> ratio.interpolate(min as Double, max as Double) as T
	is Interpolable<*> -> (min as Interpolable<Any>).interpolateWith(max as Interpolable<Any>, ratio) as T
	else -> throw IllegalArgumentException("Value is not interpolable")
}
