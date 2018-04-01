package com.soywiz.korma.random

actual object BaseRand {
	actual fun random(): Double = java.util.Random().nextDouble()
}