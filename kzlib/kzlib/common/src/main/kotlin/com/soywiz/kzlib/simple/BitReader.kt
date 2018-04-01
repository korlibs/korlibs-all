package com.soywiz.kzlib.simple

interface BitReader {
	fun readBits(bitcount: Int): Int
	fun alignbyte(): Unit
	fun u8(): Int
	val available: Int
}

fun BitReader.u16_le(): Int = (this.u8() shl 0) or (this.u8() shl 8)
fun BitReader.u32_le(): Int = (this.u8() shl 0) or (this.u8() shl 8) or (this.u8() shl 16) or (this.u8() shl 24)

fun BitReader.u16_be(): Int = (this.u8() shl 8) or (this.u8() shl 0)
fun BitReader.u32_be(): Int = (this.u8() shl 24) or (this.u8() shl 16) or (this.u8() shl 8) or (this.u8() shl 0)

fun BitReader.readBit() = readBits(1) != 0
fun BitReader.bytes(count: Int) = ByteArray(count).apply {
	for (n in 0 until count) this[n] = u8().toByte()
}

fun BitReader.strz(): String {
	return MemorySyncStreamToByteArray {
		while (true) {
			val c = u8()
			if (c == 0) break
			write8(c)
		}
	}.toASCIIString()
}

class ArrayBitReader(val data: ByteArray) : BitReader {
	private var offset = 0
	private var bitdata = 0
	private var bitsavailable = 0

	override fun alignbyte() {
		this.bitsavailable = 0
	}

	val length get() = data.size
	override val available get() = length - offset

	override fun u8(): Int = this.data[this.offset++].toInt() and 0xFF

	override fun readBits(bitcount: Int): Int {
		while (bitcount > this.bitsavailable) {
			this.bitdata = this.bitdata or (this.u8() shl this.bitsavailable)
			this.bitsavailable += 8
		}
		val readed = this.bitdata and ((1 shl bitcount) - 1)
		this.bitdata = this.bitdata ushr bitcount
		this.bitsavailable -= bitcount
		return readed
	}
}
