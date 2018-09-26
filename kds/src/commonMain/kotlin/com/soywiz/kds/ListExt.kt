package com.soywiz.kds

fun <T> List<T>.getCyclic(index: Int) = this[index % this.size]
fun <T> Array<T>.getCyclic(index: Int) = this[index % this.size]