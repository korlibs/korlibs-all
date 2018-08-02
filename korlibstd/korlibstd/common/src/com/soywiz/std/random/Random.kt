package com.soywiz.std.random

expect object Random {
	fun nextInt(): Int
}

fun Random.nextDouble(): Double = (nextInt() and 0x7FFFFFFF).toDouble() / 0x7FFFFFFF.toDouble()
fun Random.nextLong(): Long = (nextInt().toLong() shl 32) or nextInt().toLong()
