package com.soywiz.korio.compression

import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

interface CompressionMethod {
	suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream): Unit = unsupported()
	suspend fun compress(i: AsyncInputWithLengthStream, o: AsyncOutputStream): Unit = unsupported()
}

suspend fun CompressionMethod.compress(data: ByteArray): ByteArray {
	return MemorySyncStreamToByteArray {
		compress(data.openAsync(), this.toAsync())
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

