package com.soywiz.korio.compression

import com.soywiz.korio.*

typealias SyncCompression = KorioNative.SyncCompression

//fun SyncCompression.inflateTo(input: ByteArray, output: ByteArray): Int {
//	val uncompressed = SyncCompression.inflate(input)
//	val size = min(uncompressed.size, output.size)
//	arraycopy(uncompressed, 0, output, 0, size)
//	return size
//}
