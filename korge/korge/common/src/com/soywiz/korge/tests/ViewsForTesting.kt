package com.soywiz.korge.tests

import com.soywiz.klock.*
import com.soywiz.korge.view.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.timeunit.*
import kotlin.coroutines.experimental.*

open class ViewsForTesting {
	val testDispatcher = TestCoroutineDispatcher(KorioDefaultDispatcher)

	val viewsLog = ViewsLog(testDispatcher).apply {
		syncTest { init() }
	}
	val injector = viewsLog.injector
	val ag = viewsLog.ag
	val input = viewsLog.input
	val views = viewsLog.views

	fun syncTest(block: suspend TestCoroutineDispatcher.() -> Unit): Unit = Korio(testDispatcher) {
		block(testDispatcher)
	}

	fun viewsTest(step: TimeSpan = 10.milliseconds, callback: suspend TestCoroutineDispatcher.() -> Unit) = syncTest {
		callback()
	}


}
