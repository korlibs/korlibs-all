package com.soywiz.korio.async

import kotlin.coroutines.experimental.*

@Deprecated("", ReplaceWith("coroutineContext", "kotlin.coroutines.experimental.coroutineContext"), level = DeprecationLevel.ERROR)
suspend fun getCoroutineContext() = coroutineContext