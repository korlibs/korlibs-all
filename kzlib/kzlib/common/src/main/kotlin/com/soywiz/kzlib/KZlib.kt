package com.soywiz.kzlib

object KZlib {
	val VERSION = KZLIB_VERSION
}

fun ByteArray.inflate(nowrap: Boolean = false): ByteArray {
	val bos = ByteArrayOutputStream()
	InflaterInputStream(ByteArrayInputStream(this), nowrap).copyTo(bos)
	return bos.toByteArray()
}

fun ByteArray.deflate(level: Int = 7, nowrap: Boolean = false): ByteArray {
	val bos = ByteArrayOutputStream()
	val deflater = Deflater(level, nowrap)
	val def = DeflaterOutputStream(bos, deflater)
	def.write(this)
	def.flush()
	def.finish()
	deflater.end()
	return bos.toByteArray()
}

