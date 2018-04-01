package com.soywiz.korau.format.util

import com.soywiz.korio.lang.format

class BitBuffer(length: Int) : IBitReader {
	// Store bits as ints for faster reading
	private val bits: IntArray = IntArray(length)
	private var readIndex: Int = 0
	private var writeIndex: Int = 0
	var bitsRead: Int = 0; private set
	var bitsWritten: Int = 0; private set

	val bytesRead: Int get() = bitsRead.ushr(3)
	val bytesWritten: Int get() = bitsWritten.ushr(3)

	override fun read1(): Int {
		bitsRead++
		val bit = bits[readIndex]
		readIndex++
		if (readIndex >= bits.size) readIndex = 0

		return bit
	}

	override fun read(n: Int): Int {
		var n = n
		var value = 0
		while (n > 0) {
			value = (value shl 1) + read1()
			n--
		}
		return value
	}

	override fun skip(n: Int) {
		bitsRead += n
		readIndex += n
		while (readIndex < 0) readIndex += bits.size
		while (readIndex >= bits.size) readIndex -= bits.size
	}

	private fun writeBit(n: Int) {
		bits[writeIndex] = n
		writeIndex++
		bitsWritten++
		if (writeIndex >= bits.size) writeIndex = 0
	}

	fun writeByte(n: Int) {
		for (bit in 7 downTo 0) writeBit(n shr bit and 0x1)
	}

	override fun readBool(): Boolean = read1() != 0

	override fun peek(n: Int): Int {
		val read = read(n)
		skip(-n)
		return read
	}

	override fun toString(): String =
		"BitBuffer readIndex=%d, writeIndex=%d, readCount=%d".format(readIndex, writeIndex, bitsRead)
}
