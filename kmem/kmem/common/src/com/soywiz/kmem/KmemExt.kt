package com.soywiz.kmem

fun arraycopyAny(src: Any, srcPos: Int, dst: Any, dstPos: Int, size: Int): Unit {
	when (src) {
		is ByteArray -> arraycopy(src, srcPos, dst as ByteArray, dstPos, size)
		//is CharArray -> arraycopy(src, srcPos, dst as CharArray, dstPos, size)
		is ShortArray -> arraycopy(src, srcPos, dst as ShortArray, dstPos, size)
		is IntArray -> arraycopy(src, srcPos, dst as IntArray, dstPos, size)
		is FloatArray -> arraycopy(src, srcPos, dst as FloatArray, dstPos, size)
		is DoubleArray -> arraycopy(src, srcPos, dst as DoubleArray, dstPos, size)
		is LongArray -> arraycopy(src, srcPos, dst as LongArray, dstPos, size)
		is MemBuffer -> arraycopy(src, srcPos, dst as MemBuffer, dstPos, size)
		else -> error("Not a valid array $src")
	}
}

fun DataBufferAlloc(size: Int) = MemBufferAlloc(size).getData()
