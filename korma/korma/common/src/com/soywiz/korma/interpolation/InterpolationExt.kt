package com.soywiz.korma.interpolation

fun Double.interpolate(l: Float, r: Float): Float = (l + (r - l) * this).toFloat()
fun Double.interpolate(l: Double, r: Double): Double = l + (r - l) * this
fun Double.interpolate(l: Int, r: Int): Int = (l + (r - l) * this).toInt()
fun Double.interpolate(l: Long, r: Long): Long = (l + (r - l) * this).toLong()
fun <T : Interpolable<T>> Double.interpolate(l: T, r: T): T = l.interpolateWith(r, this)
fun <T> Double.interpolate(l: Interpolable<T>, r: Interpolable<T>): T = l.interpolateWith(r as T, this)
