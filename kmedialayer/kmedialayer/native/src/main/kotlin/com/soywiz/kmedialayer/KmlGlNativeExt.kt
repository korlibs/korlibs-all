package com.soywiz.kmedialayer

import kotlinx.cinterop.*

fun Boolean.narrow(): Byte = (if (this) 1 else 0).toByte()
fun Byte.toBool(): Boolean = this.toInt() != 0

fun Int.narrowSize(): Long = this.toLong() // For 64-bit
fun Float.narrowFloat(): Double = this.toDouble() // For 64-bit
