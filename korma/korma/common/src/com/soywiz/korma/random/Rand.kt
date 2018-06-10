package com.soywiz.korma.random

interface Rand {
	val maxValue: Int
	fun seed(s: Int): Rand
	fun nextInt(): Int
}