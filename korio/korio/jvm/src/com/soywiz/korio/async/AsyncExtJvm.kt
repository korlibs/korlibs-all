package com.soywiz.korio.async

import com.soywiz.korio.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.Deferred
import java.util.concurrent.*

fun <T> Deferred<T>.jvmSyncAwait(): T = runBlocking { await() }

operator fun ExecutorService.invoke(callback: () -> Unit) {
	this.execute(callback)
}

@Deprecated("")
suspend fun <T> executeInNewThread(task: suspend () -> T): T = KorioNative.executeInWorker(task)

@Deprecated("")
suspend fun <T> executeInWorkerJvm(task: suspend () -> T): T = KorioNative.executeInWorker(task)
