package com.soywiz.korio.lang

// @TODO: takeIf and takeUnless2 Not supported in kotlin-native 0.7.1
inline fun <T> T.takeIf2(predicate: (T) -> Boolean): T? = if (predicate(this)) this else null
inline fun <T> T.takeUnless2(predicate: (T) -> Boolean): T? = if (!predicate(this)) this else null
