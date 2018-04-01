package com.soywiz.kmedialayer

import kotlinx.cinterop.*
import platform.posix.*

suspend fun loadFileBytesAsync(path: String, range: LongRange?): ByteArray {
	//ioWorker.schedule(TransferMode.CHECKED, {
	//    LoadInfo(path, range)
	//}, { info ->
	//    loadFileBytesSync(info.path, info.range)
	//})
	return loadFileBytesSync(path, range)
}

data class LoadInfo(val path: String, val range: LongRange?)

val ioWorker by lazy { startWorker() }

fun loadFileBytesSync(path: String, range: LongRange?): ByteArray {
	val file = fopen(path, "rb") ?: throw RuntimeException("Can't open file $path")
	fseek(file, 0, SEEK_END)
	val endPos = ftell(file)
	//println("endPos: $endPos")
	val start = range?.start ?: 0L
	val count = range?.endInclusive?.minus(1) ?: (endPos - start)
	fseek(file, start.narrow(), SEEK_SET)
	//println("seek: ${start}")
	val bytes = memScoped {
		val ptr = allocArray<ByteVar>(count)
		val readCount = fread(ptr, 1, count.narrow(), file).toInt()
		//println("count: ${count}")
		//println("readCount: $readCount")
		ptr.readBytes(readCount)
	}
	fclose(file)
	return bytes
}
