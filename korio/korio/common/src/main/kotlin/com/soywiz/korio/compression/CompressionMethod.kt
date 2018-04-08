package com.soywiz.korio.compression

import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

class CompressionContext(var level: Int = 6) {
	var name: String? = null
	var custom: Any? = null
}

interface CompressionMethod {
	suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream): Unit = unsupported()
	suspend fun compress(
		i: AsyncInputWithLengthStream,
		o: AsyncOutputStream,
		context: CompressionContext = CompressionContext(level = 6)
	): Unit = unsupported()
}

suspend fun CompressionMethod.compress(
	data: ByteArray,
	context: CompressionContext = CompressionContext(level = 6)
): ByteArray {
	return MemorySyncStreamToByteArray {
		compress(data.openAsync(), this.toAsync(), context)
	}
}

suspend fun CompressionMethod.uncompress(data: ByteArray): ByteArray {
	return MemorySyncStreamToByteArray {
		uncompress(data.openAsync(), this.toAsync())
	}
}

suspend fun CompressionMethod.uncompressTo(data: ByteArray, out: AsyncOutputStream): AsyncOutputStream {
	uncompress(data.openAsync(), out)
	return out
}

