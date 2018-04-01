package com.soywiz.korge.native

import kotlin.reflect.*

expect object KorgeNative {
	fun getClassSimpleName(clazz: KClass<*>): String
}
