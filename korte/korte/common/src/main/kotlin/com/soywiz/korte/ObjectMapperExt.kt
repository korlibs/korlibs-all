package com.soywiz.korte

import kotlin.reflect.*
import com.soywiz.korio.serialization.*

suspend fun ObjectMapper.hasProperty(instance: Any?, name: String): Boolean {
	TODO()
}

suspend fun ObjectMapper.get(instance: Any?, key: Any?): Any? {
	TODO()
}

suspend fun ObjectMapper.set(instance: Any?, key: Any?, value: Any?) {
	TODO()
}

suspend fun ObjectMapper.hasMethod(instance: Any?, name: String): Boolean {
	TODO()
}

suspend fun ObjectMapper.invokeAsync(clazz: KClass<*>, instance: Any?, name: String, args: List<Any?>): Any? {
	TODO()
}
