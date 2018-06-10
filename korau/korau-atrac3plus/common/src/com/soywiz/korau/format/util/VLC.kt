package com.soywiz.korau.format.util

import com.soywiz.kmem.*
import com.soywiz.korau.format.atrac3plus.Atrac3plusDecoder
import com.soywiz.korio.lang.format
import kotlin.math.max
import kotlin.math.min

class VLC {
	var bits: Int = 0
	var table: Array<IntArray>? = null
	var tableSize: Int = 0
	var tableAllocated: Int = 0

	private class VLCcode : Comparable<VLCcode> {
		internal var bits: Int = 0
		internal var symbol: Int = 0
		internal var code: Int = 0

		override fun compareTo(o: VLCcode): Int = this.code.ushr(1) - o.code.ushr(1)
	}

	fun initVLCSparse(bits: IntArray, codes: IntArray, symbols: IntArray): Int =
		initVLCSparse(bits.size, codes.size, bits, codes, symbols)

	fun initVLCSparse(nbBits: Int, nbCodes: Int, bits: IntArray, codes: IntArray, symbols: IntArray?): Int {
		var nbCodes = nbCodes
		val buf = arrayOfNulls<VLCcode>(nbCodes + 1)

		this.bits = nbBits

		var j = 0
		for (i in 0 until nbCodes) {
			val vlCcode = VLCcode()
			buf[j] = vlCcode
			vlCcode.bits = bits[i]
			if (vlCcode.bits <= nbBits) {
				continue
			}
			if (vlCcode.bits > 3 * nbBits || vlCcode.bits > 32) {
				log.error { "Too long VLC (%d) in initVLC".format(vlCcode.bits) }
				return -1
			}
			vlCcode.code = codes[i]
			if (vlCcode.code >= 1 shl vlCcode.bits) {
				log.error { "Invalid code in initVLC" }
				return -1
			}
			vlCcode.code = vlCcode.code shl 32 - vlCcode.bits
			if (symbols != null) {
				vlCcode.symbol = symbols[i]
			} else {
				vlCcode.symbol = i
			}
			j++
		}

		@Suppress("UNCHECKED_CAST")
		Arrays_sort(buf as Array<VLC.VLCcode>, 0, j)

		for (i in 0 until nbCodes) {
			val vlCcode = VLCcode()
			buf[j] = vlCcode
			vlCcode.bits = bits[i]
			if (!(vlCcode.bits != 0 && vlCcode.bits <= nbBits)) {
				continue
			}
			vlCcode.code = codes[i]
			vlCcode.code = vlCcode.code shl 32 - vlCcode.bits
			if (symbols != null) {
				vlCcode.symbol = symbols[i]
			} else {
				vlCcode.symbol = i
			}
			j++
		}

		nbCodes = j

		@Suppress("UNCHECKED_CAST")
		return buildTable(nbBits, nbCodes, buf as Array<VLC.VLCcode>, 0)
	}

	private fun <T : Comparable<T>> Arrays_sort(buf: Array<T>, fromIndex: Int, toIndex: Int) {
		val sorted = buf.copyOfRange(fromIndex, toIndex).sortedArray()
		arraycopy(sorted, 0, buf, fromIndex, toIndex - fromIndex)
	}

	private fun buildTable(tableNbBits: Int, nbCodes: Int, codes: Array<VLCcode>, codeOffset: Int): Int {
		val tableSize = 1 shl tableNbBits
		if (tableNbBits > 30) {
			return -1
		}

		val tableIndex = allocTable(tableSize)
		if (tableIndex < 0) {
			return tableIndex
		}

		// first pass: map codes and compute auxiliary table sizes
		run {
			var i = 0
			while (i < nbCodes) {
				var n = codes[codeOffset + i].bits
				var code = codes[codeOffset + i].code
				val symbol = codes[codeOffset + i].symbol
				if (n <= tableNbBits) {
					// no need to add another table
					var j = code.ushr(32 - tableNbBits)
					val nb = 1 shl tableNbBits - n
					val inc = 1
					for (k in 0 until nb) {
						val bits = table!![tableIndex + j][1]
						if (bits != 0 && bits != n) {
							log.error { "incorrect codes" }
							return -1
						}
						table!![tableIndex + j][1] = n //bits
						table!![tableIndex + j][0] = symbol
						j += inc
					}
				} else {
					// fill auxiliary table recursively
					n -= tableNbBits
					val codePrefix = code.ushr(32 - tableNbBits)
					var subtableBits = n
					codes[codeOffset + i].bits = n
					codes[codeOffset + i].code = code shl tableNbBits
					var k: Int
					k = i + 1
					while (k < nbCodes) {
						n = codes[codeOffset + k].bits - tableNbBits
						if (n <= 0) {
							break
						}
						code = codes[codeOffset + k].code
						if (code.ushr(32 - tableNbBits) != codePrefix) {
							break
						}
						codes[codeOffset + k].bits = n
						codes[codeOffset + k].code = code shl tableNbBits
						subtableBits = max(subtableBits, n)
						k++
					}
					subtableBits = min(subtableBits, tableNbBits)
					table!![tableIndex + codePrefix][1] = -subtableBits
					val index = buildTable(subtableBits, k - i, codes, codeOffset + i)
					if (index < 0) {
						return index
					}
					table!![tableIndex + codePrefix][0] = index //code
					i = k - 1
				}
				i++
			}
		}

		for (i in 0 until tableSize) {
			if (table!![tableIndex + i][1] == 0) { //bits
				table!![tableIndex + i][0] = -1 //codes
			}
		}

		return tableIndex
	}

	private fun allocTable(size: Int): Int {
		val index = tableSize

		tableSize += size
		tableAllocated = tableSize
		val newTable = Array(tableAllocated) { IntArray(2) }
		if (table != null) {
			for (i in 0 until index) {
				newTable[i][0] = table!![i][0]
				newTable[i][1] = table!![i][1]
			}
		}
		table = newTable

		return index
	}

	/**
	 * Parse a vlc code.
	 * @param bits is the number of bits which will be read at once, must be
	 * identical to nb_bits in init_vlc()
	 * @param maxDepth is the number of times bits bits must be read to completely
	 * read the longest vlc code
	 * = (max_vlc_length + bits - 1) / bits
	 */
	fun getVLC2(br: IBitReader, maxDepth: Int = 1): Int {
		var nbBits: Int
		var index = br.peek(bits)
		var code = table!![index][0]
		var n = table!![index][1]

		if (maxDepth > 1 && n < 0) {
			br.skip(bits)

			nbBits = -n

			index = br.peek(nbBits) + code
			code = table!![index][0]
			n = table!![index][1]
			if (maxDepth > 2 && n < 0) {
				br.skip(nbBits)

				nbBits = -n

				index = br.peek(nbBits) + code
				code = table!![index][0]
				n = table!![index][1]
			}
		}
		br.skip(n)

		return code
	}

	companion object {
		private val log = Atrac3plusDecoder.log
	}
}
