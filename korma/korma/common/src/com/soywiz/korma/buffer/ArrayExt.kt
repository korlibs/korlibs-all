package com.soywiz.korma.buffer

fun IntArray.copyTo(srcPos: Int, dst: IntArray, dstPos: Int, count: Int) {
	for (n in 0 until count) dst[dstPos + n] = this[srcPos + n]
}

fun DoubleArray.copyTo(srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int) {
	for (n in 0 until count) dst[dstPos + n] = this[srcPos + n]
}

fun IntArray.binarySearch(v: Int, fromIndex: Int, toIndex: Int): Int {
	var low = fromIndex
	var high = toIndex - 1

	while (low <= high) {
		val mid = (low + high) / 2
		val mval = this[mid]

		if (mval < v) {
			low = mid + 1
		} else if (mval > v) {
			high = mid - 1
		} else {
			return mid
		}
	}
	return -low - 1
}

fun DoubleArray.binarySearch(v: Double, fromIndex: Int, toIndex: Int): Int {
	var low = fromIndex
	var high = toIndex - 1

	while (low <= high) {
		val mid = (low + high) / 2
		val mval = this[mid]

		if (mval < v) {
			low = mid + 1
		} else if (mval > v) {
			high = mid - 1
		} else {
			return mid
		}
	}
	return -low - 1
}
