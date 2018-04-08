package com.soywiz.korio.compression.deflate

import com.soywiz.korio.compression.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*

object GZIP : CompressionMethod {
	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
		val s = BitReader(i)
		if (s.u8() != 31 || s.u8() != 139) error("Not a GZIP file")
		val method = s.u8()
		if (method != 8) error("Just supported deflate in GZIP")
		val ftext = s.readBit()
		val fhcrc = s.readBit()
		val fextra = s.readBit()
		val fname = s.readBit()
		val fcomment = s.readBit()
		val reserved = s.readBits(3)
		val mtime = s.u32_le()
		val xfl = s.u8()
		val os = s.u8()
		val extra = if (fextra) s.bytes(s.u16_le()) else byteArrayOf()
		val name = if (fname) s.strz() else null
		val comment = if (fcomment) s.strz() else null
		val crc16 = if (fhcrc) s.u16_le() else 0
		var chash = CRC32.INITIAL
		Deflate.uncompress(s, object : AsyncOutputStream {
			override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
				chash = CRC32.update(chash, buffer, offset, offset + len)
				o.write(buffer, offset, len)
			}

			override suspend fun close() = o.close()

		})
		val crc32 = s.u32_le()
		val size = s.u32_le()
		if (chash != crc32) invalidOp("CRC32 doesn't match ${chash.hex32} != ${crc32.hex32}")
	}

	override suspend fun compress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
		TODO()
	}
}
