package com.soywiz.kds

fun <T> MutableList<T>.splice(removeOffset: Int, removeCount: Int, vararg itemsToAdd: T) {
	// @TODO: Improve performance
	for (n in 0 until removeCount) this.removeAt(removeOffset)
	for (n in 0 until itemsToAdd.size) {
		this.add(removeOffset + n, itemsToAdd[n])
	}
}

fun <T, R> Iterable<T>.reduceAcumulate(init: R, reductor: (R, T) -> R): R {
	var acc = init
	for (item in this) acc = reductor(acc, item)
	return acc
}
