package com.soywiz.korinject.util

fun syncTest(block: suspend () -> Unit): Unit = syncTestImpl(ignoreJs = false, block = block)
fun syncTestIgnoreJs(block: suspend () -> Unit): Unit = syncTestImpl(ignoreJs = true, block = block)
