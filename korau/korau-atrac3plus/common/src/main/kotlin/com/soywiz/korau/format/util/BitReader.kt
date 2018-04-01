package com.soywiz.korau.format.util

import com.soywiz.korio.lang.format
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readU8

/*
class BitReader(val mem: SyncStream) : IBitReader {
	val addr: Int get() = mem.position.toInt()
	val size: Int get() = mem.available.toInt()
	val initialAddr: Int = addr

	private var bits: Int = 0
	private var value: Int = 0
	private var direction: Int = 1

	val bitsLeft: Int get() = (size shl 3) + bits

	val bytesRead: Int
		get() {
			var bytesRead = addr - initialAddr
			if (bits == 8) bytesRead--
			return bytesRead
		}

	val bitsRead: Int get() = (addr - initialAddr) * 8 - bits

	override fun readBool(): Boolean = read1() != 0

	override fun read1(): Int {
		if (bits <= 0) {
			value = mem.readU8()
			bits = 8
		}
		val bit = value shr 7
		bits--
		value = value shl 1 and 0xFF

		return bit
	}

	override fun read(n: Int): Int {
		var n = n
		var read: Int
		if (n <= bits) {
			read = value shr 8 - n
			bits -= n
			value = value shl n and 0xFF
		} else {
			read = 0
			while (n > 0) {
				read = (read shl 1) + read1()
				n--
			}
		}

		return read
	}

	override fun peek(n: Int): Int {
		val read = read(n)
		skip(-n)
		return read
	}

	override fun skip(n: Int) {
		bits -= n
		if (n >= 0) {
			while (bits < 0) {
				mem.readU8()
				bits += 8
			}
		} else {
			while (bits > 8) {
				addr -= direction
				size++
				bits -= 8
			}
		}

		if (bits > 0) {
			value = mem.read8(addr - direction)
			value = value shl 8 - bits and 0xFF
		}
	}

	override fun toString(): String =
		"BitReader addr=0x%08X, bits=%d, size=0x%X, bits read %d".format(addr, bits, size, bitsRead)
}
*/


class BitReader(val mem: IMemory, private var addr: Int, private var size: Int) : IBitReader {
	private val initialAddr: Int = addr
	private val initialSize: Int = size
	private var bits: Int = 0
	private var value: Int = 0
	private var direction: Int = 1

	val bitsLeft: Int get() = (size shl 3) + bits

	val bytesRead: Int
		get() {
			var bytesRead = addr - initialAddr
			if (bits == 8) bytesRead--
			return bytesRead
		}

	val bitsRead: Int get() = (addr - initialAddr) * 8 - bits

	override fun readBool(): Boolean = read1() != 0

	override fun read1(): Int {
		if (bits <= 0) {
			value = mem.read8(addr)
			addr += direction
			size--
			bits = 8
		}
		val bit = value shr 7
		bits--
		value = value shl 1 and 0xFF

		return bit
	}

	override fun read(n: Int): Int {
		var n = n
		var read: Int
		if (n <= bits) {
			read = value shr 8 - n
			bits -= n
			value = value shl n and 0xFF
		} else {
			read = 0
			while (n > 0) {
				read = (read shl 1) + read1()
				n--
			}
		}

		return read
	}

	fun readByte(): Int {
		if (bits == 8) {
			bits = 0
			return value
		}
		if (bits > 0) {
			skip(bits)
		}
		val read = mem.read8(addr)
		addr += direction
		size--

		return read
	}

	override fun peek(n: Int): Int {
		val read = read(n)
		skip(-n)
		return read
	}

	override fun skip(n: Int) {
		bits -= n
		if (n >= 0) {
			while (bits < 0) {
				addr += direction
				size--
				bits += 8
			}
		} else {
			while (bits > 8) {
				addr -= direction
				size++
				bits -= 8
			}
		}

		if (bits > 0) {
			value = mem.read8(addr - direction)
			value = value shl 8 - bits and 0xFF
		}
	}

	fun seek(n: Int) {
		addr = initialAddr + n
		size = initialSize - n
		bits = 0
	}

	fun setDirection(direction: Int) {
		this.direction = direction
		bits = 0
	}

	fun byteAlign() {
		if (bits > 0 && bits < 8) {
			skip(bits)
		}
	}

	override fun toString(): String =
		"BitReader addr=0x%08X, bits=%d, size=0x%X, bits read %d".format(addr, bits, size, bitsRead)
}
