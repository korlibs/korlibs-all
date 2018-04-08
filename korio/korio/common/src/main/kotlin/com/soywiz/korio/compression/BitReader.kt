package com.soywiz.korio.compression

import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

open class BitReader(val s: AsyncInputWithLengthStream) {
	private var offset = 0
	private var bitdata = 0
	private var bitsavailable = 0

	suspend fun alignbyte() {
		this.bitsavailable = 0
	}

	suspend fun available() = s.getLength() - offset

	suspend fun u8(): Int = s.readU8()

	suspend fun readBits(bitcount: Int): Int {
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

suspend fun BitReader.u16_le(): Int = (this.u8() shl 0) or (this.u8() shl 8)
suspend fun BitReader.u32_le(): Int = (this.u8() shl 0) or (this.u8() shl 8) or (this.u8() shl 16) or (this.u8() shl 24)

suspend fun BitReader.u16_be(): Int = (this.u8() shl 8) or (this.u8() shl 0)
suspend fun BitReader.u32_be(): Int = (this.u8() shl 24) or (this.u8() shl 16) or (this.u8() shl 8) or (this.u8() shl 0)

suspend fun BitReader.readBit() = readBits(1) != 0
suspend fun BitReader.bytes(count: Int) = ByteArray(count).apply {
	for (n in 0 until count) this[n] = u8().toByte()
}

suspend fun BitReader.strz(): String {
	return MemorySyncStreamToByteArray {
		while (true) {
			val c = u8()
			if (c == 0) break
			write8(c)
		}
	}.toString(ASCII)
}
