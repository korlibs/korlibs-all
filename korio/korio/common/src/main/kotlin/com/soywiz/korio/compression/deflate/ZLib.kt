package com.soywiz.korio.compression.deflate

import com.soywiz.korio.compression.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*

object ZLib : CompressionMethod {
	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
		val s = BitReader(i)
		//println("Zlib.uncompress.available[0]:" + s.available())
		s.prepareBytesUpTo(64)
		val compressionMethod = s.readBits(4)
		if (compressionMethod != 8) error("Invalid zlib stream compressionMethod=$compressionMethod")
		val windowBits = (s.readBits(4) + 8)
		val fcheck = s.readBits(5)
		val hasDict = s.sreadBit()
		val flevel = s.readBits(2)
		var dictid = 0
		if (hasDict) {
			dictid = s.su32_le()
			TODO("Unsupported custom dictionaries (Provided DICTID=$dictid)")
		}

		//s.alignbyte()
		var chash = Adler32.INITIAL
		Deflate(windowBits).uncompress(s, object : AsyncOutputStream by o {
			override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
				o.write(buffer, offset, len)
				chash = Adler32.update(chash, buffer, offset, len)
				//println("UNCOMPRESS:'" + buffer.sliceArray(offset until (offset + len)).toString(UTF8) + "':${chash.hex32}")
			}
		})

		s.prepareBytesUpTo(4)
		val adler32 = s.su32_be()
		//println("Zlib.uncompress.available[1]:" + s.available())
		if (chash != adler32) invalidOp("Adler32 doesn't match ${chash.hex32} != ${adler32.hex32}")
	}

	override suspend fun compress(
		i: AsyncInputWithLengthStream,
		o: AsyncOutputStream,
		context: CompressionContext
	) {
		val slidingBits = 15
		o.write8(0x8 or ((slidingBits - 8) shl 4)) // METHOD=8, BITS=7+8
		o.write8(0x00 or (context.level shl 6)) // FCHECK=0, HASDICT=0, LEVEL = context.level

		var chash = Adler32.INITIAL
		Deflate(slidingBits).compress(object : AsyncInputWithLengthStream by i {
			override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
				val read = i.read(buffer, offset, len)
				if (read > 0) {
					chash = Adler32.update(chash, buffer, offset, read)
					//println("COMPRESS:'" + buffer.sliceArray(offset until (offset + len)).toString(UTF8) + "':${chash.hex32}")
				}
				return read
			}
		}, o)
		o.write32_be(chash)
	}
}
