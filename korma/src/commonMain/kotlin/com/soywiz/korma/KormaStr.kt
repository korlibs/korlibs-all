package com.soywiz.korma

object KormaStr {
	val Float.niceStr: String get() = if (this.toLong().toFloat() == this) "${this.toLong()}" else "$this"
	val Double.niceStr: String get() = if (this.toLong().toDouble() == this) "${this.toLong()}" else "$this"
}

inline fun <T> KormaStr(callback: KormaStr.() -> T) = callback(KormaStr)
