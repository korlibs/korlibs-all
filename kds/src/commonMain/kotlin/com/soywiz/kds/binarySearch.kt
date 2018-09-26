package com.soywiz.kds

fun IntArray.binarySearch(v: Int, fromIndex: Int, toIndex: Int): Int {
	var low = fromIndex
	var high = toIndex - 1

	while (low <= high) {
		val mid = (low + high) / 2
		val mval = this[mid]

		when {
			mval < v -> low = mid + 1
			mval > v -> high = mid - 1
			else -> return mid
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

		when {
			mval < v -> low = mid + 1
			mval > v -> high = mid - 1
			else -> return mid
		}
	}
	return -low - 1
}
