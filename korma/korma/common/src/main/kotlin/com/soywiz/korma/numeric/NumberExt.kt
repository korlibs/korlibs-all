package com.soywiz.korma.numeric

val Float.niceStr: String get() = if (this.toLong().toFloat() == this) "${this.toLong()}" else "$this"
val Double.niceStr: String get() = if (this.toLong().toDouble() == this) "${this.toLong()}" else "$this"
