package com.soywiz.korio.compression

import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

open class BitReader(val s: AsyncInputWithLengthStream) {
	private var bitdata = 0
	private var bitsavailable = 0

	suspend fun alignbyte(): BitReader {
		this.bitsavailable = 0
		return this
	}

	suspend fun hasBitsAvailable() = bitsavailable > 0 || s.getAvailable() > 0
	suspend fun available() = s.getAvailable()

	suspend fun u8(): Int = alignbyte().s.readU8()

	suspend fun readBits(bitcount: Int): Int {
		while (bitcount > this.bitsavailable) {
			this.bitdata = this.bitdata or (s.readU8() shl this.bitsavailable)
			this.bitsavailable += 8
		}
		val readed = this.bitdata and ((1 shl bitcount) - 1)
		this.bitdata = this.bitdata ushr bitcount
		this.bitsavailable -= bitcount
		return readed
	}
}

suspend fun BitReader.u16_le(): Int = alignbyte().s.readU16_le()
suspend fun BitReader.u32_le(): Int = alignbyte().s.readS32_le()
suspend fun BitReader.u16_be(): Int = alignbyte().s.readU16_be()
suspend fun BitReader.u32_be(): Int = alignbyte().s.readS32_be()

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