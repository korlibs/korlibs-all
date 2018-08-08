package com.soywiz.korio.compression.deflate

import com.soywiz.kmem.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

object FastDeflate {
	// https://www.ietf.org/rfc/rfc1951.txt
	private val FIXED_TREE: HuffmanTree = HuffmanTree().fromLengths(IntArray(288).apply {
		for (n in 0..143) this[n] = 8
		for (n in 144..255) this[n] = 9
		for (n in 256..279) this[n] = 7
		for (n in 280..287) this[n] = 8
	})
	private val FIXED_DIST: HuffmanTree = HuffmanTree().fromLengths(IntArray(32) { 5 })

	private val LEN_EXTRA = intArrayOf(
		0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 0, 0
	)

	private val LEN_BASE = intArrayOf(
		3, 4, 5, 6, 7, 8, 9, 10, 11, 13,
		15, 17, 19, 23, 27, 31, 35, 43, 51, 59,
		67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
	)

	private val DIST_EXTRA = intArrayOf(
		0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
	)

	private val DIST_BASE = intArrayOf(
		1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
		257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577, 0, 0
	)

	private val HCLENPOS = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

	fun zlibUncompress(i: ByteArray, offset: Int = 0, expectedOutSize: Int = 64): ByteArray {
		val s = BitReader(i, offset)
		val compressionMethod = s.bits(4)
		if (compressionMethod != 8) error("Invalid zlib stream compressionMethod=$compressionMethod")
		val windowBits = (s.bits(4) + 8)
		val fcheck = s.bits(5)
		val hasDict = s.bit()
		val flevel = s.bits(2)
		var dictid = 0
		if (hasDict) run { dictid = s.u32be(); TODO("Unsupported custom dictionaries (Provided DICTID=$dictid)") }
		val out = uncompress(windowBits, s, ByteArrayBuilder.Small(expectedOutSize)).toByteArray()
		val chash = Adler32.update(Adler32.INITIAL, out, 0, out.size)
		val adler32 = s.u32be()
		if (chash != adler32) invalidOp("Adler32 doesn't match ${chash.hex} != ${adler32.hex}")
		return out
	}

	fun uncompress(windowBits: Int, i: ByteArray, offset: Int = 0): ByteArray {
		return uncompress(windowBits, BitReader(i, offset)).toByteArray()
		//println("uncompress[6]")
	}

	fun uncompress(
		windowBits: Int,
		reader: BitReader,
		out: ByteArrayBuilder.Small = ByteArrayBuilder.Small()
	): ByteArrayBuilder.Small = out.apply {
		val temp = TempState()
		val dynTree = HuffmanTree()
		val dynDist = HuffmanTree()
		val ring = SlidingWindow(windowBits)
		val sout = SlidingWindowWithOutput(ring, out)
		var lastBlock = false
		while (!lastBlock) {
			lastBlock = reader.bit()
			val btype = reader.bits(2)
			if (btype !in 0..2) error("invalid bit")
			if (btype == 0) {
				reader.discardBits()
				val len = reader.u16le()
				val nlen = reader.u16le()
				val nnlen = nlen.inv() and 0xFFFF
				if (len != nnlen) error("Invalid deflate stream: len($len) != ~nlen($nnlen) :: nlen=$nlen")
				sout.putOut(reader.i, reader.alignedBytes(len), len)
			} else {
				val tree: HuffmanTree
				val dist: HuffmanTree
				if (btype == 1) {
					tree = FIXED_TREE
					dist = FIXED_DIST
				} else {
					tree = dynTree
					dist = dynDist
					readDynamicTree(reader, temp, tree, dist)
				}
				while (true) {
					val value = tree.sreadOneValue(reader)
					if (value == 256) break
					if (value < 256) {
						sout.putOut(value.toByte())
					} else {
						val zlenof = value - 257
						val lengthExtra = reader.bits(LEN_EXTRA[zlenof])
						val distanceData = dist.sreadOneValue(reader)
						val distanceExtra = reader.bits(DIST_EXTRA[distanceData])
						val distance = DIST_BASE[distanceData] + distanceExtra
						val length = LEN_BASE[zlenof] + lengthExtra
						sout.getPutCopyOut(distance, length)
					}
				}
			}
		}
	}

	private fun readDynamicTree(reader: BitReader, temp: TempState, l: HuffmanTree, r: HuffmanTree) {
		val hlit = reader.bits(5) + 257
		val hdist = reader.bits(5) + 1
		val hclen = reader.bits(4) + 4
		val codeLenCodeLen = IntArray(19)
		for (i in 0 until hclen) codeLenCodeLen[HCLENPOS[i]] = reader.bits(3)
		//console.info(codeLenCodeLen);
		val codeLen = temp.codeLen.fromLengths(codeLenCodeLen)
		val lengths = IntArray(hlit + hdist)
		var n = 0
		val hlithdist = hlit + hdist
		while (n < hlithdist) {
			val value = codeLen.sreadOneValue(reader)
			if (value !in 0..18) error("Invalid")

			val len = when (value) {
				16 -> reader.bits(2) + 3
				17 -> reader.bits(3) + 3
				18 -> reader.bits(7) + 11
				else -> 1
			}
			val vv = when (value) {
				16 -> lengths[n - 1]
				17 -> 0
				18 -> 0
				else -> value
			}

			lengths.fill(vv, n, n + len)
			n += len
		}
		l.fromLengths(lengths, 0, hlit)
		r.fromLengths(lengths, hlit, lengths.size)
	}

	class TempState {
		val codeLen = HuffmanTree()
	}

	open class BitReader(val i: ByteArray, var offset: Int = 0) {
		var bitdata = 0
		var bitsavailable = 0
		var peekbits = 0

		fun discardBits(): BitReader {
			bitdata = 0
			bitsavailable = 0
			offset -= peekbits / 8
			peekbits = 0
			return this
		}

		fun peek(bitcount: Int): Int {
			while (bitsavailable < bitcount) {
				bitdata = bitdata or (u8() shl bitsavailable)
				bitsavailable += 8
				peekbits += 8
			}
			return bitdata and ((1 shl bitcount) - 1)
		}

		fun bits(bitcount: Int): Int {
			val read = peek(bitcount)
			peekbits -= bitcount
			bitdata = bitdata ushr bitcount
			bitsavailable -= bitcount
			return read
		}

		fun bit(): Boolean = bits(1) != 0

		private fun u8(): Int = i.getOrElse(offset++) { 0 }.toInt() and 0xFF

		fun u16le(): Int {
			val l = u8()
			val h = u8()
			return (h shl 8) or (l)
		}

		fun u32be(): Int {
			val v3 = u8()
			val v2 = u8()
			val v1 = u8()
			val v0 = u8()
			return (v3 shl 24) or (v2 shl 16) or (v1 shl 8) or (v0)
		}

		fun alignedBytes(count: Int): Int {
			discardBits()
			return offset.apply { offset += count }
		}
	}

	class SlidingWindowWithOutput(val sliding: SlidingWindow, val out: ByteArrayBuilder.Small) {
		fun getPutCopyOut(distance: Int, length: Int) {
			for (n in 0 until length) {
				val v = sliding.getPut(distance)
				out.append(v.toByte())
			}
		}

		fun putOut(bytes: ByteArray, offset: Int, len: Int) {
			out.append(bytes, offset, len)
			sliding.putBytes(bytes, offset, len)
		}

		fun putOut(byte: Byte) {
			out.append(byte)
			sliding.put(byte.toUnsigned())
		}
	}

	class SlidingWindow(nbits: Int) {
		val data = ByteArray(1 shl nbits)
		val mask = data.size - 1
		var pos = 0

		fun get(offset: Int): Int {
			return data[(pos - offset) and mask].toInt() and 0xFF
		}

		fun getPut(offset: Int): Int = put(get(offset))

		fun put(value: Int): Int {
			data[pos] = value.toByte()
			pos = (pos + 1) and mask
			return value
		}

		fun putBytes(bytes: ByteArray, offset: Int, len: Int) {
			for (n in 0 until len) put(bytes[offset + n].toUnsigned())
		}
	}

	// @TODO: Compute fast decodings with a lookup table and bit peeking for 9 bits
	class HuffmanTree {
		fun sreadOneValue(reader: BitReader): Int {
			//val value = reader.peek(9)

			return sreadOneValueSlow(reader)
		}

		private fun sreadOneValueSlow(reader: BitReader): Int {
			var node = this.root
			do {
				node = if (reader.bit()) node.right else node.left
			} while (node != NIL && node.value == INVALID_VALUE)
			return node.value
		}

		private val INVALID_VALUE = -1
		private val NIL = 1023

		private val value = IntArray(1024)
		private val len = IntArray(1024)
		private val left = IntArray(1024)
		private val right = IntArray(1024)

		private var nodeOffset = 0
		private var root: Int = NIL
		private var symbolLimit: Int = 0

		private fun resetAlloc() {
			nodeOffset = 0
		}

		private fun alloc(value: Int, left: Int, right: Int): Int {
			return (nodeOffset++).apply {
				this@HuffmanTree.value[this] = value
				this@HuffmanTree.left[this] = left
				this@HuffmanTree.right[this] = right
			}
		}

		private fun allocLeaf(value: Int): Int = alloc(value, NIL, NIL)
		private fun allocNode(left: Int, right: Int): Int = alloc(INVALID_VALUE, left, right)

		private val Int.value get() = this@HuffmanTree.value[this]
		private val Int.len get() = this@HuffmanTree.len[this]
		private val Int.left get() = this@HuffmanTree.left[this]
		private val Int.right get() = this@HuffmanTree.right[this]

		private val MAX_LEN = 16
		private val COUNTS = IntArray(MAX_LEN + 1)
		private val OFFSETS = IntArray(MAX_LEN + 1)
		private val COFFSET = IntArray(MAX_LEN + 1)
		private val CODES = IntArray(288)
		//private val CODES = IntArray(512)
		fun fromLengths(codeLengths: IntArray, start: Int = 0, end: Int = codeLengths.size): HuffmanTree {
			var oldOffset = 0
			var oldCount = 0
			val codeLengthsSize = end - start

			resetAlloc()

			COUNTS.fill(0)

			// Compute the count of codes per length
			for (n in start until end) {
				val codeLen = codeLengths[n]
				if (codeLen !in 0..MAX_LEN) error("Invalid HuffmanTree.codeLengths $codeLen")
				COUNTS[codeLen]++
			}

			// Compute the disposition using the counts per length
			var currentOffset = 0
			for (n in 0 until MAX_LEN) {
				val count = COUNTS[n]
				OFFSETS[n] = currentOffset
				COFFSET[n] = currentOffset
				currentOffset += count
			}

			// Place elements in the computed disposition
			for (n in start until end) {
				val codeLen = codeLengths[n]
				CODES[COFFSET[codeLen]++] = n - start
			}

			for (i in MAX_LEN downTo 1) {
				val newOffset = nodeOffset

				val OFFSET = OFFSETS[i]
				val SIZE = COUNTS[i]
				for (j in 0 until SIZE) allocLeaf(CODES[OFFSET + j])
				for (j in 0 until oldCount step 2) allocNode(oldOffset + j, oldOffset + j + 1)

				oldOffset = newOffset
				oldCount = SIZE + oldCount / 2
				if (oldCount % 2 != 0) error("This canonical code does not represent a Huffman code tree: $oldCount")
			}
			if (oldCount != 2) error("This canonical code does not represent a Huffman code tree")
			this.root = allocNode(nodeOffset - 2, nodeOffset - 1)
			this.symbolLimit = codeLengthsSize
			return this
		}
	}
}
