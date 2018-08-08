package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.klock.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.scheduling.*
import kotlinx.coroutines.experimental.scheduling.ExperimentalCoroutineDispatcher
import kotlinx.coroutines.experimental.timeunit.TimeUnit
import java.io.*
import java.util.concurrent.*
import kotlin.coroutines.experimental.*

// @TODO:
actual val KorioDefaultDispatcher: CoroutineDispatcher = newSingleThreadContext("KorioDefaultDispatcher")
//actual val KorioDefaultDispatcher: CoroutineDispatcher = DefaultDispatcher
