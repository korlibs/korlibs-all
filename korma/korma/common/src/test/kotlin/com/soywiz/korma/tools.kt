package com.soywiz.korma

import kotlin.math.abs
import kotlin.test.assertTrue

fun assertEqualsDouble(l: Double, r: Double, delta: Double) {
	assertTrue(abs(l - r) < delta, message = "$l != $r :: delta=$delta")
}

