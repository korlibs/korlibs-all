package com.soywiz.korio.compression

object SimpleInflater {
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
	private val infos_lz = mapOf(
		257 to Info(0, 3),
		258 to Info(0, 4),
		259 to Info(0, 5),
		260 to Info(0, 6),
		261 to Info(0, 7),
		262 to Info(0, 8),
		263 to Info(0, 9),
		264 to Info(0, 10),
		265 to Info(1, 11),
		266 to Info(1, 13),
		267 to Info(1, 15),
		268 to Info(1, 17),
		269 to Info(2, 19),
		270 to Info(2, 23),
		271 to Info(2, 27),
		272 to Info(2, 31),
		273 to Info(3, 35),
		274 to Info(3, 43),
		275 to Info(3, 51),
		276 to Info(3, 59),
		277 to Info(4, 67),
		278 to Info(4, 83),
		279 to Info(4, 99),
		280 to Info(4, 115),
		281 to Info(5, 131),
		282 to Info(5, 163),
		283 to Info(5, 195),
		284 to Info(5, 227),
		285 to Info(0, 258)
	)
	private val infos_lz2 = arrayOf(
		Info(0, 1),
		Info(0, 2),
		Info(0, 3),
		Info(0, 4),
		Info(1, 5),
		Info(1, 7),
		Info(2, 9),
		Info(2, 13),
		Info(3, 17),
		Info(3, 25),
		Info(4, 33),
		Info(4, 49),
		Info(5, 65),
		Info(5, 97),
		Info(6, 129),
		Info(6, 193),
		Info(7, 257),
		Info(7, 385),
		Info(8, 513),
		Info(8, 769),
		Info(9, 1025),
		Info(9, 1537),
		Info(10, 2049),
		Info(10, 3073),
		Info(11, 4097),
		Info(11, 6145),
		Info(12, 8193),
		Info(12, 12289),
		Info(13, 16385),
		Info(13, 24577)
	)
	private val HCLENPOS = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

	fun inflateZlib(data: ByteArray): ByteArray {
		return this.inflateZlibBitReader(ArrayBitReader(data))
	}

	fun inflateRaw(data: ByteArray): ByteArray {
		return this.inflateRawBitReader(ArrayBitReader(data))
	}

	fun inflateGzip(data: ByteArray): ByteArray {
		return this.inflateGzipBitReader(ArrayBitReader(data))
	}

	fun inflateGzipBitReader(reader: BitReader): ByteArray {
		if (reader.u8() != 31) error("Not a GZIP file")
		if (reader.u8() != 139) error("Not a GZIP file")
		val method = reader.u8()
		if (method != 8) error("Just supported deflate in GZIP")
		val ftext = reader.readBit()
		val fhcrc = reader.readBit()
		val fextra = reader.readBit()
		val fname = reader.readBit()
		val fcomment = reader.readBit()
		val reserved0 = reader.readBit()
		val reserved1 = reader.readBit()
		val reserved2 = reader.readBit()
		var MTIME = reader.u32()
		var XFL = reader.u8()
		var OS = reader.u8()

		var EXTRA = if (fextra) reader.bytes(reader.u16()) else byteArrayOf()
		var name = if (fname) reader.strz() else null
		var comment = if (fcomment) reader.strz() else null
		var CRC16 = if (fhcrc) reader.u16() else 0
		//val uncompressed = inflateZlibBitReader(reader)
		val uncompressed = inflateRawBitReader(reader, windowBits = 15)
		var CRC32 = reader.u32()
		var size = reader.u32()
		return uncompressed
	}

	fun inflateZlibBitReader(reader: BitReader): ByteArray {
		val compressionMethod = reader.readBits(4)
		if (compressionMethod != 8) error("Invalid zlib stream compressionMethod=$compressionMethod")
		val windowBits = (reader.readBits(4) + 8)
		var fcheck = reader.readBits(5)
		val hasDict = reader.readBits(1) != 0
		var flevel = reader.readBits(2)
		if (hasDict) error("Not implemented HAS DICT")
		return this.inflateRawBitReader(reader, windowBits)
	}

	fun inflateRawBitReader(reader: BitReader, windowBits: Int = 15): ByteArray {
		return MemorySyncStreamToByteArray {
			inflateRawBitReaderOut(reader, SlidingWindow(windowBits), this)
		}
	}

	private inline fun debug(callback: () -> String) {
		//println(callback())
	}

	fun inflateRawBitReaderOut(reader: BitReader, ring: SlidingWindow, out: SyncOutputStream) {
		val fixedtree = SimpleInflater.fixedtree
		val fixeddist = SimpleInflater.fixeddist
		val infos_lz = SimpleInflater.infos_lz
		val infos_lz2 = SimpleInflater.infos_lz2
		var lastBlock = false
		while (!lastBlock) {
			lastBlock = reader.readBits(1) != 0
			val btype = reader.readBits(2)
			debug { "LAST_BLOCK: $lastBlock" }
			debug { "BTYPE: $btype" }
			when (btype) {
				0 -> {
					reader.alignbyte()
					val len = reader.u16()
					val nlen = reader.u16()
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
						val HCLENPOS = SimpleInflater.HCLENPOS
						val HLIT = reader.readBits(5) + 257 // hlit  + 257
						val HDIST = reader.readBits(5) + 1 // hdist +   1
						val HCLEN = reader.readBits(4) + 4 // hclen +   4
						val codeLenCodeLen = IntArray(19)
						for (i in 0 until HCLEN) codeLenCodeLen[HCLENPOS[i]] = reader.readBits(3)
						//console.info(codeLenCodeLen);
						val codeLen = HuffmanTree.fromLengths(codeLenCodeLen)
						val lengths = IntArray(HLIT + HDIST)
						var n = 0
						while (n < HLIT + HDIST) {
							var value = codeLen.readOneValue(reader)
							var len = 1
							when {
								value < 16 -> len = 1
								value == 16 -> {
									value = lengths[n - 1]
									len = reader.readBits(2) + 3
								}
								value == 17 -> {
									value = 0
									len = reader.readBits(3) + 3
								}
								value == 18 -> {
									value = 0
									len = reader.readBits(7) + 11
								}
								else -> error("Invalid")
							}
							for (c in 0 until len) lengths[n++] = value
						}
						tree = HuffmanTree.fromLengths(lengths.sliceArray(0 until HLIT))
						dist = HuffmanTree.fromLengths(lengths.sliceArray(HLIT until lengths.size))
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
								val lengthInfo = infos_lz[value]!!
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
}

class SlidingWindow(val nbits: Int) {
	val data = ByteArray(1 shl nbits)
	val mask = data.size - 1
	var pos = 0

	fun get(offset: Int): Int {
		return data[(pos - offset) and mask].toInt() and 0xFF
	}

	fun put(value: Int) {
		data[pos] = value.toByte()
		pos = (pos + 1) and mask
	}
}

class HuffmanNode(val value: Int, val len: Int, val left: HuffmanNode?, val right: HuffmanNode?) {
	val isLeaf get() = this.len != 0

	companion object {
		fun leaf(value: Int, len: Int) = HuffmanNode(value, len, null, null)
		fun int(left: HuffmanNode, right: HuffmanNode) = HuffmanNode(-1, 0, left, right)
	}
}

class HuffmanTree(val root: HuffmanNode, val symbolLimit: Int) {
	data class Result(var value: Int, var bitcode: Int, var bitcount: Int)

	fun readOne(reader: BitReader, out: Result = Result(0, 0, 0)): Result {
		//console.log('-------------');
		var node: HuffmanNode? = this.root
		var bitcount = 0
		var bitcode = 0
		do {
			val bbit = reader.readBits(1)
			val bit = (bbit != 0)
			bitcode = bitcode or (bbit shl bitcount)
			bitcount++
			//console.log('bit', bit);
			node = if (bit) node!!.right else node!!.left
			//console.info(node);
		} while (node != null && node.len == 0)
		if (node == null) error("NODE = NULL")
		return out.apply {
			this.value = node.value
			this.bitcode = bitcode
			this.bitcount = bitcount
		}
	}

	private val tempResult = Result(0, 0, 0)
	fun readOneValue(reader: BitReader) = readOne(reader, tempResult).value

	companion object {
		fun fromLengths(codeLengths: IntArray): HuffmanTree {
			var nodes = arrayListOf<HuffmanNode>()
			for (i in (codeLengths.max() ?: 0) downTo 1) {
				// Descend through positive code lengths
				val newNodes = arrayListOf<HuffmanNode>()

				// Add leaves for symbols with code length i
				for (j in 0 until codeLengths.size) {
					if (codeLengths[j] == i) newNodes.add(HuffmanNode.leaf(j, i))
				}

				// Merge nodes from the previous deeper layer
				for (j in 0 until nodes.size step 2) {
					newNodes.add(HuffmanNode.int(nodes[j], nodes[j + 1]))
				}

				nodes = newNodes
				if (nodes.size % 2 != 0) error("This canonical code does not represent a Huffman code tree: ${nodes.size}")
			}

			if (nodes.size != 2) error("This canonical code does not represent a Huffman code tree")

			return HuffmanTree(HuffmanNode.int(nodes[0], nodes[1]), codeLengths.size)

		}
	}
}

interface BitReader {
	fun readBits(bitcount: Int): Int
	fun alignbyte(): Unit
	fun u8(): Int
	fun u16(): Int
	fun u32(): Int
	val available: Int
}

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
	override fun u16(): Int = this.u8() or (this.u8() shl 8)
	override fun u32(): Int = this.u8() or (this.u8() shl 8) or (this.u8() shl 16) or (this.u8() shl 24)

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

////////////////////////////

fun ByteArray.toASCIIString(): String {
	var out = ""
	for (n in 0 until size) out += this[n].toChar()
	return out
}

interface SyncOutputStream {
	fun write8(v: Int)
}

class ByteArraySyncOutputStream : SyncOutputStream {
	var out = ByteArray(1024)
	var size = 0
	private val available get() = capacity - size
	val capacity get() = out.size

	private fun ensure(count: Int) {
		if (available < count) {
			out = out.copyOf(out.size * 2 + count)
		}
	}

	override fun write8(v: Int) {
		ensure(1)
		out[size++] = v.toByte()
	}

	fun toByteArray() = out.copyOf(size)
}

inline fun MemorySyncStreamToByteArray(callback: ByteArraySyncOutputStream.() -> Unit): ByteArray =
	ByteArraySyncOutputStream().apply(callback).toByteArray()

private fun unhex(c: Char): Int = when (c) {
	in '0'..'9' -> 0 + (c - '0')
	in 'a'..'f' -> 10 + (c - 'a')
	in 'A'..'F' -> 10 + (c - 'A')
	else -> throw RuntimeException("Illegal HEX character $c")
}

val String.unhex: ByteArray
	get() {
		val str = this
		val out = ByteArray(str.length / 2)
		var m = 0
		for (n in 0 until out.size) {
			out[n] = ((unhex(str[m++]) shl 4) or unhex(str[m++])).toByte()
		}
		return out
	}
