package com.soywiz.kds

class BitSet(val size: Int) {
	val data = IntArray(KdsExt { size divCeil 4 })

	private fun part(index: Int) = index ushr 5
	private fun bit(index: Int) = index and 0x1f

	operator fun get(index: Int): Boolean = ((data[part(index)] ushr (bit(index))) and 1) != 0
	operator fun set(index: Int, value: Boolean): Unit {
		val i = part(index)
		val b = bit(index)
		if (value) {
			data[i] = data[i] or (1 shl b)
		} else {
			data[i] = data[i] and (1 shl b).inv()
		}
	}

	fun set(index: Int): Unit = set(index, true)
	fun unset(index: Int): Unit = set(index, false)

	fun clear(): Unit = MemTools.fill(data, 0)
}