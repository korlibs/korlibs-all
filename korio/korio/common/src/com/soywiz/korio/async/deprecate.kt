package com.soywiz.korio.async

import kotlin.coroutines.experimental.*

@Deprecated("", ReplaceWith("coroutineContext", "kotlin.coroutines.coroutineContext"), level = DeprecationLevel.ERROR)
suspend fun getCoroutineContext() = coroutineContext