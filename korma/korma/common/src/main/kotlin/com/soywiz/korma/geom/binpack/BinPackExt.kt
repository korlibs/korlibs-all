package com.soywiz.korma.geom.binpack

import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.Size

fun <T> BinPack.addBatch(items: Iterable<T>, getSize: (T) -> Size): List<Pair<T, Rectangle?>> {
	val its = items.toList()
	val out = hashMapOf<T, Rectangle?>()
	val sorted = its.map { it to getSize(it) }.sortedByDescending { it.second.area }
	for ((i, size) in sorted) out[i] = this.add(size.width, size.height)
	return its.map { it to out[it] }
}