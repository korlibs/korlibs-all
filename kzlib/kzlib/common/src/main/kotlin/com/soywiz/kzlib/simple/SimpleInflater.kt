package com.soywiz.kzlib.simple

// https://www.ietf.org/rfc/rfc1950.txt // ZLIB Compressed Data Format Specification version 3.3
// https://www.ietf.org/rfc/rfc1951.txt // DEFLATE Compressed Data Format Specification version 1.3
// https://www.ietf.org/rfc/rfc1952.txt // GZIP file format specification version 4.3
object SimpleInflater {
	fun inflateZlib(data: ByteArray): ByteArray = inflateZlibBitReader(ArrayBitReader(data))
	fun inflateRaw(data: ByteArray): ByteArray = inflateRawBitReader(ArrayBitReader(data))
	fun inflateGzip(data: ByteArray): ByteArray = inflateGzipBitReader(ArrayBitReader(data))

	fun inflateGzipBitReader(reader: BitReader): ByteArray {
		if (reader.u8() != 31 || reader.u8() != 139) error("Not a GZIP file")
		val method = reader.u8()
		if (method != 8) error("Just supported deflate in GZIP")
		val ftext = reader.readBit()
		val fhcrc = reader.readBit()
		val fextra = reader.readBit()
		val fname = reader.readBit()
		val fcomment = reader.readBit()
		val reserved = reader.readBits(3)
		var mtime = reader.u32_le()
		var xfl = reader.u8()
		var os = reader.u8()
		var extra = if (fextra) reader.bytes(reader.u16_le()) else byteArrayOf()
		var name = if (fname) reader.strz() else null
		var comment = if (fcomment) reader.strz() else null
		var crc16 = if (fhcrc) reader.u16_le() else 0
		val uncompressed = inflateRawBitReader(reader, windowBits = 15)
		val crc32 = reader.u32_le()
		var size = reader.u32_le()
		checkChecksum("crc32", crc32, SimpleChecksum.crc32(uncompressed))
		return uncompressed
	}

	fun inflateZlibBitReader(reader: BitReader): ByteArray {
		val compressionMethod = reader.readBits(4)
		if (compressionMethod != 8) error("Invalid zlib stream compressionMethod=$compressionMethod")
		val windowBits = (reader.readBits(4) + 8)
		var fcheck = reader.readBits(5)
		val hasDict = reader.readBit()
		var flevel = reader.readBits(2)
		val sliding = SlidingWindow(windowBits)
		if (hasDict) {
			val dictid = reader.u32_le()
			TODO("Unsupported custom dictionaries (Provided DICTID=$dictid)")
		}

		//val compressed = reader.bytes(reader.available - 4)
		//val uncompressed = inflateRawBitReader(ArrayBitReader(compressed), sliding)
		val uncompressed = inflateRawBitReader(reader, sliding)

		//println(reader.available)
		val adler32 = reader.u32_be()
		//println(reader.available)
		checkChecksum("adler32", adler32, SimpleChecksum.adler32(uncompressed))
		return uncompressed
	}

	private fun checkChecksum(name: String, expected: Int, actual: Int) {
		if (expected != actual) error("$name(${expected.hex32}) doesn't match computed_$name(${actual.hex32})")
	}

	fun inflateRawBitReader(reader: BitReader, ring: SlidingWindow): ByteArray {
		return MemorySyncStreamToByteArray { inflateRawBitReaderOut(reader, ring, this) }
	}

	fun inflateRawBitReader(reader: BitReader, windowBits: Int = 15): ByteArray {
		return inflateRawBitReader(reader, SlidingWindow(windowBits))
	}

	private inline fun debug(callback: () -> String) {
		//println(callback())
	}

	fun inflateRawBitReaderOut(reader: BitReader, ring: SlidingWindow, out: SyncOutputStream) {
		var lastBlock = false
		while (!lastBlock) {
			lastBlock = reader.readBits(1) != 0
			val btype = reader.readBits(2)
			debug { "LAST_BLOCK: $lastBlock" }
			debug { "BTYPE: $btype" }
			when (btype) {
				0 -> {
					reader.alignbyte()
					val len = reader.u16_le()
					val nlen = reader.u16_le()
					debug { "UNCOMPRESSED: $len" }
					if (len != nlen.inv()) error("Invalid file: len($len) != ~nlen(${nlen.inv()}) :: nlen=$nlen")
					for (n in 0 until len) {
						val v = reader.u8()
						out.write8(v)
						ring.put(v)
					}
				}
				1, 2 -> {
					val tree: HuffmanTree
					val dist: HuffmanTree
					if (btype == 1) {
						debug { "HUFFMAN_FIXED" }
						tree = fixedtree
						dist = fixeddist
					} else {
						debug { "HUFFMAN_DYNAMIC" }
						val hclenpos = HCLENPOS
						val hlit = reader.readBits(5) + 257 // hlit  + 257
						val hdist = reader.readBits(5) + 1 // hdist +   1
						val hclen = reader.readBits(4) + 4 // hclen +   4
						val codeLenCodeLen = IntArray(19)
						for (i in 0 until hclen) codeLenCodeLen[hclenpos[i]] = reader.readBits(3)
						//console.info(codeLenCodeLen);
						val codeLen = HuffmanTree.fromLengths(codeLenCodeLen)
						val lengths = IntArray(hlit + hdist)
						var n = 0
						while (n < hlit + hdist) {
							var value = codeLen.readOneValue(reader)
							var len = 1
							when {
								value < 16 -> len = 1
								value == 16 -> run { value = lengths[n - 1]; len = reader.readBits(2) + 3 }
								value == 17 -> run { value = 0; len = reader.readBits(3) + 3 }
								value == 18 -> run { value = 0; len = reader.readBits(7) + 11 }
								else -> error("Invalid")
							}
							for (c in 0 until len) lengths[n++] = value
						}
						tree = HuffmanTree.fromLengths(lengths.sliceArray(0 until hlit))
						dist = HuffmanTree.fromLengths(lengths.sliceArray(hlit until lengths.size))
					}
					var completed = false
					while (!completed && reader.available > 0) {
						val value = tree.readOneValue(reader)
						when {
							value < 256 -> {
								debug { "$value" }
								out.write8(value)
								ring.put(value)
							}
							value == 256 -> completed = true
							else -> {
								val lengthInfo = infos_lz[value - 257]
								val lengthExtra = reader.readBits(lengthInfo.extra)
								val length = lengthInfo.offset + lengthExtra

								val distanceData = dist.readOneValue(reader)
								val distanceInfo = infos_lz2[distanceData]
								val distanceExtra = reader.readBits(distanceInfo.extra)
								val distance = distanceInfo.offset + distanceExtra

								for (n in 0 until length) {
									val v = ring.get(distance)
									out.write8(v)
									ring.put(v)
								}
							}
						}
					}
				}
				3 -> {
					error("invalid bit")
				}
			}
		}
	}

	private data class Info(val extra: Int, val offset: Int)

	private val lengths0: IntArray by lazy {
		IntArray(288).apply {
			for (n in 0..143) this[n] = 8
			for (n in 144..255) this[n] = 9
			for (n in 256..279) this[n] = 7
			for (n in 280..287) this[n] = 8
		}
	}

	// https://www.ietf.org/rfc/rfc1951.txt
	private val fixedtree: HuffmanTree by lazy { HuffmanTree.fromLengths(lengths0) }
	private val fixeddist: HuffmanTree by lazy { HuffmanTree.fromLengths(IntArray(32) { 5 }) }

	private val infos_lz = arrayOf(
		Info(0, 3), Info(0, 4), Info(0, 5), Info(0, 6),
		Info(0, 7), Info(0, 8), Info(0, 9), Info(0, 10),
		Info(1, 11), Info(1, 13), Info(1, 15), Info(1, 17),
		Info(2, 19), Info(2, 23), Info(2, 27), Info(2, 31),
		Info(3, 35), Info(3, 43), Info(3, 51), Info(3, 59),
		Info(4, 67), Info(4, 83), Info(4, 99), Info(4, 115),
		Info(5, 131), Info(5, 163), Info(5, 195), Info(5, 227), Info(0, 258)
	)
	private val infos_lz2 = arrayOf(
		Info(0, 1), Info(0, 2), Info(0, 3), Info(0, 4),
		Info(1, 5), Info(1, 7), Info(2, 9), Info(2, 13),
		Info(3, 17), Info(3, 25), Info(4, 33), Info(4, 49),
		Info(5, 65), Info(5, 97), Info(6, 129), Info(6, 193),
		Info(7, 257), Info(7, 385), Info(8, 513), Info(8, 769),
		Info(9, 1025), Info(9, 1537), Info(10, 2049), Info(10, 3073),
		Info(11, 4097), Info(11, 6145), Info(12, 8193), Info(12, 12289),
		Info(13, 16385), Info(13, 24577)
	)
	private val HCLENPOS = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)
}
