package com.soywiz.kds

fun <T> List<T>.getCyclic(index: Int) = this[index % this.size]