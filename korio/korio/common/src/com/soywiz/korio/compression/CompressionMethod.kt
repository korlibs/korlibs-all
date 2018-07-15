package com.soywiz.korio.compression

import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*
import ioSync

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


object Uncompressed : CompressionMethod {
	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream): Unit = run { i.copyTo(o) }
	override suspend fun compress(
		i: AsyncInputWithLengthStream,
		o: AsyncOutputStream,
		context: CompressionContext
	): Unit = run { i.copyTo(o) }
}

suspend fun ByteArray.uncompress(method: CompressionMethod): ByteArray = method.uncompress(this)
suspend fun ByteArray.compress(
	method: CompressionMethod,
	context: CompressionContext = CompressionContext()
): ByteArray = method.compress(this, context)

fun ByteArray.syncUncompress(method: CompressionMethod): ByteArray = ioSync { method.uncompress(this) }
fun ByteArray.syncCompress(method: CompressionMethod, context: CompressionContext = CompressionContext()): ByteArray =
	ioSync { method.compress(this, context) }

fun CompressionMethod.syncUncompress(i: SyncInputStream, o: SyncOutputStream) = ioSync {
	uncompress(i.toAsyncInputStream(), o.toAsyncOutputStream())
}
