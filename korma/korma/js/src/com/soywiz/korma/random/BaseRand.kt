package com.soywiz.korma.random

actual object BaseRand {
	actual fun random(): Double = kotlin.js.Math.random()
}