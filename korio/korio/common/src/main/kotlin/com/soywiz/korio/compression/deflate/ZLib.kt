package com.soywiz.korio.compression.deflate

import com.soywiz.korio.compression.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*

object ZLib : CompressionMethod {
	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
		val s = BitReader(i)
		val compressionMethod = s.readBits(4)
		if (compressionMethod != 8) error("Invalid zlib stream compressionMethod=$compressionMethod")
		val windowBits = (s.readBits(4) + 8)
		val fcheck = s.readBits(5)
		val hasDict = s.readBit()
		val flevel = s.readBits(2)
		var dictid = 0
		if (hasDict) {
			dictid = s.u32_le()
			TODO("Unsupported custom dictionaries (Provided DICTID=$dictid)")
		}

		var chash = Adler32.INITIAL
		Deflate(windowBits).uncompress(s, object : AsyncOutputStream {
			override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
				chash = Adler32.update(chash, buffer, offset, offset + len)
				o.write(buffer, offset, len)
			}

			override suspend fun close() = o.close()
		})

		val adler32 = s.u32_be()
		if (chash != adler32) invalidOp("Adler32 doesn't match ${chash.hex32} != ${adler32.hex32}")
	}

	override suspend fun compress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
		TODO()
	}
}
