package com.soywiz.korau.format.util

import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.fill

object Arrays {
	fun <T : Comparable<T>> sort(buf: Array<T>, fromIndex: Int, toIndex: Int) {
		val sorted = buf.copyOfRange(fromIndex, toIndex).sortedArray()
		arraycopy(sorted, 0, buf, fromIndex, toIndex - fromIndex)
	}

	@Deprecated("", ReplaceWith("array.fill(i)", "com.soywiz.kmem.fill"))
	fun fill(array: IntArray, i: Int) = array.fill(i)

	@Deprecated("", ReplaceWith("array.fill(i)", "com.soywiz.kmem.fill"))
	fun fill(array: FloatArray, i: Float) = array.fill(i)

	@Deprecated("", ReplaceWith("array.fill(i, start, end)", "com.soywiz.kmem.fill"))
	fun fill(array: FloatArray, start: Int, end: Int, i: Float) = array.fill(i, start, end)
}