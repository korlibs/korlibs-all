package com.soywiz.kzlib

import org.khronos.webgl.Int32Array
import org.khronos.webgl.Int8Array

internal actual inline fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit {
	dst.unsafeCast<Int8Array>().set(src.unsafeCast<Int8Array>().subarray(srcPos, srcPos + size), dstPos)
}

internal actual inline fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int): Unit {
	dst.unsafeCast<Int32Array>().set(src.unsafeCast<Int32Array>().subarray(srcPos, srcPos + size), dstPos)
}
