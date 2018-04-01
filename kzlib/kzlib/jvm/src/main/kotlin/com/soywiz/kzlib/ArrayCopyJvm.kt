package com.soywiz.kzlib

internal actual inline fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit {
	System.arraycopy(src, srcPos, dst, dstPos, size)
}
internal actual inline fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int): Unit {
	System.arraycopy(src, srcPos, dst, dstPos, size)
}
