package com.soywiz.korio.compression.deflate

import com.soywiz.korio.compression.*
import com.soywiz.korio.stream.*
import kotlin.math.*

open class Deflate(val windowBits: Int) : CompressionMethod {
	override suspend fun compress(
		i: AsyncInputWithLengthStream,
		o: AsyncOutputStream,
		context: CompressionContext
	) {

		while (i.hasAvailable()) {
			val available = i.getAvailable()
			val chunkSize = min(available, 0xFFFFL).toInt()
			o.write8(if (chunkSize >= available) 1 else 0)
			o.write16_le(chunkSize)
			o.write16_le(chunkSize.inv())
			//for (n in 0 until chunkSize) o.write8(i.readU8())
			o.writeBytes(i.readBytesExact(chunkSize))
		}
	}

	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
		return uncompress(BitReader(i), o)
	}

	suspend fun uncompress(reader: BitReader, out: AsyncOutputStream) {
		val ring = SlidingWindow(windowBits)
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
					val nnlen = nlen.inv() and 0xFFFF
					debug { "UNCOMPRESSED: $len" }
					if (len != nnlen) error("Invalid file: len($len) != ~nlen($nnlen) :: nlen=$nlen")
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
						tree = FIXED_TREE
						dist = FIXED_DIST
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
					while (!completed && reader.hasBitsAvailable()) {
						val value = tree.readOneValue(reader)
						when {
							value < 256 -> {
								debug { "$value" }
								out.write8(value)
								ring.put(value)
							}
							value == 256 -> {
								completed = true
							}
							else -> {
								val lengthInfo = INFOS_LZ[value - 257]

								val lengthExtra = reader.readBits(lengthInfo.extra)
								val distanceData = dist.readOneValue(reader)
								val distanceInfo = INFOS_LZ2[distanceData]
								val distanceExtra = reader.readBits(distanceInfo.extra)
								val distance = distanceInfo.offset + distanceExtra
								val length = lengthInfo.offset + lengthExtra

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

	private inline fun debug(callback: () -> String) {
		//println(callback())
	}

	companion object : Deflate(15) {
		private data class Info(val extra: Int, val offset: Int)

		private val LENGTH0: IntArray by lazy {
			IntArray(288).apply {
				for (n in 0..143) this[n] = 8
				for (n in 144..255) this[n] = 9
				for (n in 256..279) this[n] = 7
				for (n in 280..287) this[n] = 8
			}
		}

		// https://www.ietf.org/rfc/rfc1951.txt
		private val FIXED_TREE: HuffmanTree by lazy { HuffmanTree.fromLengths(LENGTH0) }
		private val FIXED_DIST: HuffmanTree by lazy { HuffmanTree.fromLengths(IntArray(32) { 5 }) }

		private val INFOS_LZ = arrayOf(
			Info(0, 3), Info(0, 4), Info(0, 5), Info(0, 6),
			Info(0, 7), Info(0, 8), Info(0, 9), Info(0, 10),
			Info(1, 11), Info(1, 13), Info(1, 15), Info(1, 17),
			Info(2, 19), Info(2, 23), Info(2, 27), Info(2, 31),
			Info(3, 35), Info(3, 43), Info(3, 51), Info(3, 59),
			Info(4, 67), Info(4, 83), Info(4, 99), Info(4, 115),
			Info(5, 131), Info(5, 163), Info(5, 195), Info(5, 227), Info(0, 258)
		)
		private val INFOS_LZ2 = arrayOf(
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
}
