package com.soywiz.korio.compression.deflate

import com.soywiz.kmem.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

object FastDeflate {

		private val LENGTH0: IntArray = IntArray(288).apply {
			for (n in 0..143) this[n] = 8
			for (n in 144..255) this[n] = 9
			for (n in 256..279) this[n] = 7
			for (n in 280..287) this[n] = 8
		}

		// https://www.ietf.org/rfc/rfc1951.txt
		private val FIXED_TREE: HuffmanTree = HuffmanTree.fromLengths(LENGTH0)
		private val FIXED_DIST: HuffmanTree = HuffmanTree.fromLengths(IntArray(32) { 5 })

		private val FIXED_TREE_DIST = FIXED_TREE to FIXED_DIST

		private val stbi__zlength_extra = intArrayOf(
			0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 0, 0
		)

		private val stbi__zlength_base = intArrayOf(
			3, 4, 5, 6, 7, 8, 9, 10, 11, 13,
			15, 17, 19, 23, 27, 31, 35, 43, 51, 59,
			67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
		)

		private val stbi__zdist_extra = intArrayOf(
			0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
		)

		private val stbi__zdist_base = intArrayOf(
			1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
			257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577, 0, 0
		)

		private val HCLENPOS = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

		fun zlibUncompress(i: ByteArray, offset: Int = 0, expectedOutSize: Int = 64): ByteArray {
			val s = BitReader(i, offset)
			val compressionMethod = s.readBits(4)
			if (compressionMethod != 8) error("Invalid zlib stream compressionMethod=$compressionMethod")
			val windowBits = (s.readBits(4) + 8)
			val fcheck = s.readBits(5)
			val hasDict = s.sreadBit()
			val flevel = s.readBits(2)
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

	fun uncompress(windowBits: Int, reader: BitReader, out: ByteArrayBuilder.Small = ByteArrayBuilder.Small()): ByteArrayBuilder.Small = out.apply {
		val tempResult = HuffmanTree.Result(0, 0, 0)
		val ring = SlidingWindow(windowBits)
		val sout = SlidingWindowWithOutput(ring, out)
		var lastBlock = false
		while (!lastBlock) {
			lastBlock = reader.sreadBit()
			val btype = reader.readBits(2)
			if (btype !in 0..2) error("invalid bit")
			if (btype == 0) {
				reader.discardBits()
				val len = reader.u16le()
				val nlen = reader.u16le()
				val nnlen = nlen.inv() and 0xFFFF
				if (len != nnlen) error("Invalid deflate stream: len($len) != ~nlen($nnlen) :: nlen=$nlen")
				sout.putOut(reader.i, reader.abytes(len), len)
			} else {
				val (tree, dist) = if (btype == 1) FIXED_TREE_DIST else readDynamicTree(reader)
				while (true) {
					val value = tree.sreadOneValue(reader, tempResult)
					if (value == 256) break
					if (value < 256) {
						sout.putOut(value.toByte())
					} else {
						val zlenof = value - 257
						val lengthExtra = reader.readBits(stbi__zlength_extra[zlenof])
						val distanceData = dist.sreadOneValue(reader, tempResult)
						val distanceExtra = reader.readBits(stbi__zdist_extra[distanceData])
						val distance = stbi__zdist_base[distanceData] + distanceExtra
						val length = stbi__zlength_base[zlenof] + lengthExtra
						sout.getPutCopyOut(distance, length)
					}
				}
			}
		}
	}

	private fun readDynamicTree(reader: BitReader): Pair<HuffmanTree, HuffmanTree> {
		val tempResult = HuffmanTree.Result(0, 0, 0)
		val hlit = reader.readBits(5) + 257
		val hdist = reader.readBits(5) + 1
		val hclen = reader.readBits(4) + 4
		val codeLenCodeLen = IntArray(19)
		for (i in 0 until hclen) codeLenCodeLen[HCLENPOS[i]] = reader.readBits(3)
		//console.info(codeLenCodeLen);
		val codeLen = HuffmanTree.fromLengths(codeLenCodeLen)
		val lengths = IntArray(hlit + hdist)
		var n = 0
		val hlithdist = hlit + hdist
		while (n < hlithdist) {
			val value = codeLen.sreadOneValue(reader, tempResult)
			if (value !in 0..18) error("Invalid")

			val len = when (value) {
				16 -> reader.readBits(2) + 3
				17 -> reader.readBits(3) + 3
				18 -> reader.readBits(7) + 11
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
		return Pair(
			HuffmanTree.fromLengths(lengths.sliceArray(0 until hlit)),
			HuffmanTree.fromLengths(lengths.sliceArray(hlit until lengths.size))
		)
	}

	open class BitReader(val i: ByteArray, var offset: Int = 0) {
		var bitdata = 0
		var bitsavailable = 0

		fun discardBits(): BitReader {
			this.bitdata = 0
			this.bitsavailable = 0
			return this
		}

		fun readBits(bitcount: Int): Int {
			while (this.bitsavailable < bitcount) {
				this.bitdata = this.bitdata or (u8() shl this.bitsavailable)
				this.bitsavailable += 8
			}
			val readed = this.bitdata and ((1 shl bitcount) - 1)
			this.bitdata = this.bitdata ushr bitcount
			this.bitsavailable -= bitcount
			return readed
		}

		fun sreadBit(): Boolean = readBits(1) != 0

		private fun u8(): Int = i[offset++].toInt() and 0xFF

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

		fun abytes(count: Int): Int {
			discardBits()
			val current = offset
			offset += count
			return current
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

	class SlidingWindow(val nbits: Int) {
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

	class HuffmanTree(val root: Node, val symbolLimit: Int) {
		class Node(val value: Int, val len: Int, val left: Node?, val right: Node?) {
			companion object {
				fun leaf(value: Int, len: Int) = Node(value, len, null, null)
				fun int(left: Node, right: Node) = Node(-1, 0, left, right)
			}
		}

		data class Result(var value: Int, var bitcode: Int, var bitcount: Int)

		fun sreadOne(reader: BitReader, out: Result = Result(0, 0, 0)): Result {
			//console.log('-------------');
			var node: Node? = this.root
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

		inline fun sreadOneValue(reader: BitReader, tempResult: Result) = sreadOne(reader, tempResult).value

		companion object {
			fun fromLengths(codeLengths: IntArray): HuffmanTree {
				var nodes = arrayListOf<Node>()
				for (i in (codeLengths.max() ?: 0) downTo 1) {
					val newNodes = arrayListOf<Node>()
					for (j in 0 until codeLengths.size) if (codeLengths[j] == i) newNodes.add(
						Node.leaf(j, i)
					)
					for (j in 0 until nodes.size step 2) newNodes.add(
						Node.int(nodes[j], nodes[j + 1])
					)
					nodes = newNodes
					if (nodes.size % 2 != 0) error("This canonical code does not represent a Huffman code tree: ${nodes.size}")
				}
				if (nodes.size != 2) error("This canonical code does not represent a Huffman code tree")
				return HuffmanTree(Node.int(nodes[0], nodes[1]), codeLengths.size)
			}
		}
	}
}

/*
// public domain zlib decode    v0.2  Sean Barrett 2006-11-18
//    simple implementation
//      - all input must be provided in an upfront buffer
//      - all output is written to a single output buffer (can malloc/realloc)
//    performance
//      - fast huffman

object FastDeflate {
	// fast-way is faster to check than jpeg huffman, but slow way is slower
	const val STBI__ZFAST_BITS  = 9 // accelerate all cases in default tables
	const val STBI__ZFAST_MASK  = ((1 shl STBI__ZFAST_BITS) - 1)

	// zlib-style huffman encoding
	// (jpegs packs from left, zlib from right, so can't share code)
	class stbi__zhuffman {
		var fast = IntArray(1 shl STBI__ZFAST_BITS)
		val firstcode = IntArray(16)
		val maxcode = IntArray(17)
		val firstsymbol = IntArray(16)
		val size = IntArray(288)
		val value = IntArray(288)
	}

	fun stbi__bitreverse16(n: Int): Int
	{
		var n = n
		n = ((n and 0xAAAA) ushr  1) or ((n and 0x5555) shl 1)
		n = ((n and 0xCCCC) ushr  2) or ((n and 0x3333) shl 2)
		n = ((n and 0xF0F0) ushr  4) or ((n and 0x0F0F) shl 4)
		n = ((n and 0xFF00) ushr  8) or ((n and 0x00FF) shl 8)
		return n;
	}

	fun stbi__bit_reverse(v: Int, bits: Int): Int
	{
		check(bits <= 16);
		// to bit reverse n bits, reverse 16 and shift
		// e.g. 11 bits, bit reverse and shift away 5
		return stbi__bitreverse16(v) ushr (16 - bits);
	}

	fun stbi__zbuild_huffman(z: stbi__zhuffman, sizelist: IntArray, num: Int)
	{
		int i, k = 0;
		int code, next_code[16], sizes[17];

		// DEFLATE spec for generating codes
		memset(sizes, 0, sizeof(sizes));
		memset(z->fast, 0, sizeof(z->fast));
		for (i= 0; i < num; ++i)
		++sizes[sizelist[i]];
		sizes[0] = 0;
		for (i= 1; i < 16; ++i)
		if (sizes[i] > (1 < < i))
			return stbi__err("bad sizes", "Corrupt PNG");
		code = 0;
		for (i= 1; i < 16; ++i) {
		next_code[i] = code;
		z->firstcode[i] = (stbi__uint16) code;
		z->firstsymbol[i] = (stbi__uint16) k;
		code = (code + sizes[i]);
		if (sizes[i])
			if (code - 1 >= (1 < < i)) return stbi__err("bad codelengths", "Corrupt PNG");
		z->maxcode[i] = code << (16-i); // preshift for inner loop
		code < <= 1;
		k += sizes[i];
	}
		z->maxcode[16] = 0x10000; // sentinel
		for (i= 0; i < num; ++i) {
		int s = sizelist [i];
		if (s) {
			int c = next_code [s] - z->firstcode[s]+z->firstsymbol[s];
			stbi__uint16 fastv =(stbi__uint16)((s < < 9) | i);
			z->size [c] = (stbi_uc) s;
			z->value[c] = (stbi__uint16) i;
			if (s <= STBI__ZFAST_BITS) {
				int j = stbi__bit_reverse (next_code[s], s);
				while (j < (1 < < STBI__ZFAST_BITS)) { z ->
					fast[j] = fastv;
					j += (1 < < s);
				}
			}
			++next_code[s];
		}
	}
		return 1;
	}

// zlib-from-memory implementation for PNG reading
//    because PNG allows splitting the zlib stream arbitrarily,
//    and it's annoying structurally to have PNG call ZLIB call PNG,
//    we require PNG read all the IDATs and combine them into a single
//    memory buffer

	typedef struct
	{
		stbi_uc * zbuffer, *zbuffer_end;
		int num_bits;
		stbi__uint32 code_buffer;

		char * zout;
		char * zout_start;
		char * zout_end;
		int z_expandable;

		stbi__zhuffman z_length, z_distance;
	} stbi__zbuf;

	stbi_inline static stbi_uc stbi__zget8(stbi__zbuf *z)
	{
		if (z->zbuffer >= z->zbuffer_end) return 0;
		return * z->zbuffer++;
	}

	static void stbi__fill_bits(stbi__zbuf *z)
	{
		do {
			STBI_ASSERT(z->code_buffer < (1U << z->num_bits));
			z->code_buffer | = (unsigned int) stbi__zget8(z) << z->num_bits;
			z->num_bits += 8;
		} while (z->num_bits <= 24);
	}

	stbi_inline static unsigned int stbi__zreceive(stbi__zbuf *z, int n)
	{
		unsigned int k;
		if (z->num_bits < n) stbi__fill_bits(z);
		k = z->code_buffer & ((1 << n)-1);
		z->code_buffer >>= n;
		z->num_bits -= n;
		return k;
	}

	static int stbi__zhuffman_decode_slowpath(stbi__zbuf *a, stbi__zhuffman *z)
	{
		int b, s, k;
		// not resolved by fast table, so compute it the slow way
		// use jpeg approach, which requires MSbits at top
		k = stbi__bit_reverse(a->code_buffer, 16);
		for (s= STBI__ZFAST_BITS + 1;; ++s)
		if (k < z->maxcode[s])
		break;
		if (s == 16) return -1; // invalid code!
		// code size is s, so:
		b = (k > >(16 - s)) - z->firstcode[s]+z->firstsymbol[s];
		STBI_ASSERT(z->size[b] == s);
		a->code_buffer >>= s;
		a->num_bits -= s;
		return z->value[b];
	}

	stbi_inline static int stbi__zhuffman_decode(stbi__zbuf *a, stbi__zhuffman *z)
	{
		int b, s;
		if (a->num_bits < 16) stbi__fill_bits(a);
		b = z->fast[a->code_buffer & STBI__ZFAST_MASK];
		if (b) {
			s = b > > 9;
			a->code_buffer >>= s;
			a->num_bits -= s;
			return b & 511;
		}
		return stbi__zhuffman_decode_slowpath(a, z);
	}

	static int stbi__zexpand(stbi__zbuf *z, char *zout, int n)  // need to make room for n bytes
	{
		char * q;
		int cur, limit, old_limit;
		z->zout = zout;
		if (!z->z_expandable) return stbi__err("output buffer limit", "Corrupt PNG");
		cur = (int)(z->zout-z->zout_start);
		limit = old_limit = (int)(z->zout_end-z->zout_start);
		while (cur + n > limit)
			limit *= 2;
		q = (char *) STBI_REALLOC_SIZED (z->zout_start, old_limit, limit);
		STBI_NOTUSED(old_limit);
		if (q == NULL) return stbi__err("outofmem", "Out of memory");
		z->zout_start = q;
		z->zout = q+cur;
		z->zout_end = q+limit;
		return 1;
	}

	val stbi__zlength_base = intArrayOf(
		3, 4, 5, 6, 7, 8, 9, 10, 11, 13,
		15, 17, 19, 23, 27, 31, 35, 43, 51, 59,
		67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
	)

	val stbi__zlength_extra = intArrayOf(
		0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 0, 0
	)

	val stbi__zdist_base = intArrayOf(
		1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
		257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577, 0, 0
	)

	val stbi__zdist_extra = intArrayOf(
		0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
	)

	static int stbi__parse_huffman_block(stbi__zbuf *a)
	{
		char * zout = a->zout;
		for (;;) {
			int z = stbi__zhuffman_decode (a, &a->z_length);
			if (z < 256) {
				if (z < 0) return stbi__err("bad huffman code", "Corrupt PNG"); // error in huffman codes
				if (zout >= a->zout_end) {
					if (!stbi__zexpand(a, zout, 1)) return 0;
					zout = a->zout;
				}
				*zout++ = (char) z;
			} else {
				stbi_uc * p;
				int len, dist;
				if (z == 256) { a ->
					zout = zout;
					return 1;
				}
				z -= 257;
				len = stbi__zlength_base[z];
				if (stbi__zlength_extra[z]) len += stbi__zreceive(a, stbi__zlength_extra[z]);
				z = stbi__zhuffman_decode(a, & a->z_distance);
				if (z < 0) return stbi__err("bad huffman code", "Corrupt PNG");
				dist = stbi__zdist_base[z];
				if (stbi__zdist_extra[z]) dist += stbi__zreceive(a, stbi__zdist_extra[z]);
				if (zout - a->zout_start < dist) return stbi__err("bad dist", "Corrupt PNG");
				if (zout + len > a->zout_end) {
					if (!stbi__zexpand(a, zout, len)) return 0;
					zout = a->zout;
				}
				p = (stbi_uc *)(zout - dist);
				if (dist == 1) { // run of one byte; common in images.
					stbi_uc v = * p;
					if (len) {
						do * zout++ = v; while (--len); }
				} else {
					if (len) {
						do * zout++ = * p ++; while (--len); }
				}
			}
		}
	}

	static int stbi__compute_huffman_codes(stbi__zbuf *a)
	{
		static stbi_uc length_dezigzag[19] = { 16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15 };
		stbi__zhuffman z_codelength;
		stbi_uc lencodes [286 + 32 + 137];//padding for maximum single op
		stbi_uc codelength_sizes [19];
		int i, n;

		int hlit = stbi__zreceive (a, 5)+257;
		int hdist = stbi__zreceive (a, 5)+1;
		int hclen = stbi__zreceive (a, 4)+4;

		memset(codelength_sizes, 0, sizeof(codelength_sizes));
		for (i= 0; i < hclen; ++i) {
		int s = stbi__zreceive (a, 3);
		codelength_sizes[length_dezigzag[i]] = (stbi_uc) s;
	}
		if (!stbi__zbuild_huffman(& z_codelength, codelength_sizes, 19)) return 0;

		n = 0;
		while (n < hlit + hdist) {
			int c = stbi__zhuffman_decode (a, &z_codelength);
			if (c < 0 || c >= 19) return stbi__err("bad codelengths", "Corrupt PNG");
			if (c < 16)
				lencodes[n++] = (stbi_uc) c;
			else if (c == 16) {
				c = stbi__zreceive(a, 2) + 3;
				memset(lencodes + n, lencodes[n - 1], c);
				n += c;
			} else if (c == 17) {
				c = stbi__zreceive(a, 3) + 3;
				memset(lencodes + n, 0, c);
				n += c;
			} else {
				STBI_ASSERT(c == 18);
				c = stbi__zreceive(a, 7) + 11;
				memset(lencodes + n, 0, c);
				n += c;
			}
		}
		if (n != hlit + hdist) return stbi__err("bad codelengths", "Corrupt PNG");
		if (!stbi__zbuild_huffman(& a->z_length, lencodes, hlit)) return 0;
		if (!stbi__zbuild_huffman(& a->z_distance, lencodes+hlit, hdist)) return 0;
		return 1;
	}

	static int stbi__parse_uncomperssed_block(stbi__zbuf *a)
	{
		stbi_uc header [4];
		int len, nlen, k;
		if (a->num_bits & 7)
		stbi__zreceive(a, a->num_bits & 7); // discard
		// drain the bit-packed data into header
		k = 0;
		while (a->num_bits > 0) {
		header[k++] = (stbi_uc)(a->code_buffer & 255); // suppress MSVC run-time check
		a->code_buffer >>= 8;
		a->num_bits -= 8;
	}
		STBI_ASSERT(a->num_bits == 0);
		// now fill header the normal way
		while (k < 4)
			header[k++] = stbi__zget8(a);
		len = header[1] * 256 + header[0];
		nlen = header[3] * 256 + header[2];
		if (nlen != (len ^ 0xffff)) return stbi__err("zlib corrupt", "Corrupt PNG");
		if (a->zbuffer+len > a->zbuffer_end) return stbi__err("read past buffer", "Corrupt PNG");
		if (a->zout+len > a->zout_end)
		if (!stbi__zexpand(a, a->zout, len)) return 0;
		memcpy(a->zout, a->zbuffer, len);
		a->zbuffer += len;
		a->zout += len;
		return 1;
	}

	static int stbi__parse_zlib_header(stbi__zbuf *a)
	{
		int cmf = stbi__zget8 (a);
		int cm = cmf & 15;
		/* int cinfo = cmf >> 4; */
		int flg = stbi__zget8 (a);
		if ((cmf * 256 + flg) % 31 != 0) return stbi__err("bad zlib header", "Corrupt PNG"); // zlib spec
		if (flg & 32) return stbi__err("no preset dict", "Corrupt PNG"); // preset dictionary not allowed in png
		if (cm != 8) return stbi__err("bad compression", "Corrupt PNG"); // DEFLATE required for png
		// window = 1 << (8 + cinfo)... but who cares, we fully buffer output
		return 1;
	}

// @TODO: should statically initialize these for optimal thread safety
	static stbi_uc stbi__zdefault_length[288], stbi__zdefault_distance[32];
	static void stbi__init_zdefaults(void)
	{
		int i;   // use <= to match clearly with spec
		for (i= 0; i <= 143; ++i)     stbi__zdefault_length[i] = 8;
		for (; i <= 255; ++i)     stbi__zdefault_length[i] = 9;
		for (; i <= 279; ++i)     stbi__zdefault_length[i] = 7;
		for (; i <= 287; ++i)     stbi__zdefault_length[i] = 8;

		for (i= 0; i <= 31; ++i)     stbi__zdefault_distance[i] = 5;
	}

	static int stbi__parse_zlib(stbi__zbuf *a, int parse_header)
	{
		int final, type;
		if (parse_header)
			if (!stbi__parse_zlib_header(a)) return 0;
		a->num_bits = 0;
		a->code_buffer = 0;
		do {
			final = stbi__zreceive(a, 1);
			type = stbi__zreceive(a, 2);
			if (type == 0) {
				if (!stbi__parse_uncomperssed_block(a)) return 0;
			} else if (type == 3) {
				return 0;
			} else {
				if (type == 1) {
					// use fixed code lengths
					if (!stbi__zdefault_distance[31]) stbi__init_zdefaults();
					if (!stbi__zbuild_huffman(& a->z_length, stbi__zdefault_length, 288)) return 0;
					if (!stbi__zbuild_huffman(& a->z_distance, stbi__zdefault_distance, 32)) return 0;
				} else {
					if (!stbi__compute_huffman_codes(a)) return 0;
				}
				if (!stbi__parse_huffman_block(a)) return 0;
			}
		} while (!final);
		return 1;
	}

	static int stbi__do_zlib(stbi__zbuf *a, char *obuf, int olen, int exp, int parse_header)
	{
		a->zout_start = obuf;
		a->zout = obuf;
		a->zout_end = obuf+olen;
		a->z_expandable = exp;

		return stbi__parse_zlib(a, parse_header);
	}

	STBIDEF char *stbi_zlib_decode_malloc_guesssize(const char *buffer, int len, int initial_size, int *outlen)
	{
		stbi__zbuf a;
		char * p = (char *) stbi__malloc (initial_size);
		if (p == NULL) return NULL;
		a.zbuffer = (stbi_uc *) buffer;
		a.zbuffer_end = (stbi_uc *) buffer +len;
		if (stbi__do_zlib(& a, p, initial_size, 1, 1)) {
		if (outlen) * outlen = (int)(a.zout - a.zout_start);
		return a.zout_start;
	} else {
		STBI_FREE(a.zout_start);
		return NULL;
	}
	}

	STBIDEF char *stbi_zlib_decode_malloc(char const *buffer, int len, int *outlen)
	{
		return stbi_zlib_decode_malloc_guesssize(buffer, len, 16384, outlen);
	}

	STBIDEF char *stbi_zlib_decode_malloc_guesssize_headerflag(const char *buffer, int len, int initial_size, int *outlen, int parse_header)
	{
		stbi__zbuf a;
		char * p = (char *) stbi__malloc (initial_size);
		if (p == NULL) return NULL;
		a.zbuffer = (stbi_uc *) buffer;
		a.zbuffer_end = (stbi_uc *) buffer +len;
		if (stbi__do_zlib(& a, p, initial_size, 1, parse_header)) {
		if (outlen) * outlen = (int)(a.zout - a.zout_start);
		return a.zout_start;
	} else {
		STBI_FREE(a.zout_start);
		return NULL;
	}
	}

	STBIDEF int stbi_zlib_decode_buffer(char *obuffer, int olen, char const *ibuffer, int ilen)
	{
		stbi__zbuf a;
		a.zbuffer = (stbi_uc *) ibuffer;
		a.zbuffer_end = (stbi_uc *) ibuffer +ilen;
		if (stbi__do_zlib(& a, obuffer, olen, 0, 1))
		return (int)(a.zout - a.zout_start);
		else
		return -1;
	}

	STBIDEF char *stbi_zlib_decode_noheader_malloc(char const *buffer, int len, int *outlen)
	{
		stbi__zbuf a;
		char * p = (char *) stbi__malloc (16384);
		if (p == NULL) return NULL;
		a.zbuffer = (stbi_uc *) buffer;
		a.zbuffer_end = (stbi_uc *) buffer +len;
		if (stbi__do_zlib(& a, p, 16384, 1, 0)) {
		if (outlen) * outlen = (int)(a.zout - a.zout_start);
		return a.zout_start;
	} else {
		STBI_FREE(a.zout_start);
		return NULL;
	}
	}

	fun stbi_zlib_decode_noheader_buffer(obuffer: ByteArray, olen: Int, ibuffer: ByteArray, ilen: Int): Int
	{
		stbi__zbuf a;
		a.zbuffer = (stbi_uc *) ibuffer;
		a.zbuffer_end = (stbi_uc *) ibuffer +ilen;
		if (stbi__do_zlib(& a, obuffer, olen, 0, 0))
		return (int)(a.zout - a.zout_start);
		else
		return -1;
	}
}
*/