package com.soywiz.korio.compression

import com.soywiz.korio.async.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.stream.*
import kotlin.coroutines.experimental.*

suspend fun ByteArray.uncompress(method: CompressionMethod): ByteArray = method.uncompress(this)
suspend fun ByteArray.compress(method: CompressionMethod): ByteArray = method.compress(this)

fun ByteArray.syncUncompress(method: CompressionMethod): ByteArray = ioSync { method.uncompress(this) }
fun ByteArray.syncCompress(method: CompressionMethod): ByteArray = ioSync { method.compress(this) }

fun ByteArray.syncUncompressTo(method: CompressionMethod, out: ByteArray) {
	ioSync { method.uncompress(this.openAsync(), out.openAsync()) }
}

