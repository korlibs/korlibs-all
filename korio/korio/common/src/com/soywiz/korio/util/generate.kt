package com.soywiz.korio.util

import kotlin.coroutines.*

// From: https://github.com/Kotlin/kotlin-coroutines/blob/master/examples/generate.kt

typealias Generator<T> = SequenceBuilder<T>

@Deprecated("", ReplaceWith("buildSequence(block)", "kotlin.coroutines.buildSequence"))
fun <T> generateSync(block: suspend Generator<T>.() -> Unit): Sequence<T> = buildSequence(block)

@Deprecated("", ReplaceWith("buildSequence(block)", "kotlin.coroutines.buildSequence"))
fun <T> generate(block: suspend Generator<T>.() -> Unit): Sequence<T> = buildSequence(block)
