package com.soywiz.kmedialayer.scene.geom

fun clamp(v: Float, min: Float, max: Float): Float = if (v < min) min else if (v > max) max else v
fun clamp(v: Double, min: Double, max: Double): Double = if (v < min) min else if (v > max) max else v
fun clamp(v: Int, min: Int, max: Int): Int = if (v < min) min else if (v > max) max else v
fun clamp(v: Long, min: Long, max: Long): Long = if (v < min) min else if (v > max) max else v
