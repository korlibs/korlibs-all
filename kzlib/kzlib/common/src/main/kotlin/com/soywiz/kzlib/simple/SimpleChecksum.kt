package com.soywiz.kzlib.simple

object SimpleChecksum {
	private val adler_base = 65521

	fun update_adler32(adler: Int, data: ByteArray, start: Int = 0, end: Int = data.size): Int {
		var s1 = (adler ushr 0) and 0xffff
		var s2 = (adler ushr 16) and 0xffff

		for (n in start until end) {
			s1 = (s1 + (data[n].toInt() and 0xFF)) % adler_base
			s2 = (s2 + s1) % adler_base
		}
		return (s2 shl 16) or s1
	}

	fun adler32(data: ByteArray, start: Int = 0, end: Int = data.size): Int = update_adler32(1, data, start, end)

	private val crc_table by lazy {
		IntArray(0x100).apply {
			val POLY = 0xEDB88320.toInt()
			for (n in 0 until 0x100) {
				var c = n
				for (k in 0 until 8) c = (if ((c and 1) != 0) POLY xor (c ushr 1) else c ushr 1)
				this[n] = c
			}

		}
	}

	fun update_crc32(crc: Int, data: ByteArray, start: Int = 0, end: Int = data.size): Int {
		var c = crc.inv()
		val table = this.crc_table
		for (n in start until end) c = table[(c xor (data[n].toInt() and 0xFF)) and 0xff] xor (c ushr 8)
		return c.inv()
	}

	fun crc32(data: ByteArray, start: Int = 0, end: Int = data.size): Int = update_crc32(0, data, start, end)
}
