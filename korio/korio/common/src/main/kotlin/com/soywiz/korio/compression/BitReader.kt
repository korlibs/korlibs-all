package com.soywiz.korio.compression

interface ByteReader2 {
	val remaining: Int
	fun readByte(): Int
}

class ByteArrayByteReader2(val ba: ByteArray) : ByteReader2 {
	var pos = 0
	override val remaining get() = ba.size - pos

	override fun readByte(): Int {
		if (remaining <= 0) error("EOF")
		return ba[pos++].toInt() and 0xFF
	}
}

class BitReader2(val br: ByteReader2) : ByteReader2 by br {
	var value = 0
	var availableBits = 0

	fun discardBits() {
		value = 0
		availableBits = 0
	}

	fun readBits(requiredBits: Int): Int {
		if (requiredBits > 24) error("Unsupported reading more than 24 bits at once")
		while (availableBits < requiredBits) {
			val b = br.readByte()
			value = value or (b shl availableBits)
			availableBits += 8
		}
		val result = value and ((1 shl requiredBits) - 1)
		value = value ushr requiredBits
		availableBits -= requiredBits
		return result
	}

	fun readBitBool(): Boolean = readBits(1) != 0
}


////////////////////////////////////////////////////////
