package com.soywiz.korge.lang

import com.soywiz.korge.native.*
import kotlin.reflect.*

val <T : Any> KClass<T>.portableSimpleName: String get() = KorgeNative.getClassSimpleName(this)
