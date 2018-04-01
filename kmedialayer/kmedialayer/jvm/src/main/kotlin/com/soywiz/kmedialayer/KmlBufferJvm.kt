package com.soywiz.kmedialayer

import java.nio.*

actual class KmlNativeBuffer constructor(val baseByteBuffer: ByteBuffer) {
	val byteBuffer = baseByteBuffer
	val shortBuffer = baseByteBuffer.asShortBuffer()
	val intBuffer = baseByteBuffer.asIntBuffer()
	val floatBuffer = baseByteBuffer.asFloatBuffer()
	actual val size: Int = baseByteBuffer.limit()

	actual constructor(size: Int) : this(
		ByteBuffer.allocateDirect(
			kmlInternalRoundUp(
				size,
				8
			)
		).order(ByteOrder.nativeOrder())
	)

	actual fun getByte(index: Int): Byte = baseByteBuffer.get(index * 1)
	actual fun setByte(index: Int, value: Byte): Unit = run { baseByteBuffer.put(index * 1, value) }
	actual fun getShort(index: Int): Short = baseByteBuffer.getShort(index * 2)
	actual fun setShort(index: Int, value: Short): Unit = run { baseByteBuffer.putShort(index * 2, value) }
	actual fun getInt(index: Int): Int = baseByteBuffer.getInt(index * 4)
	actual fun setInt(index: Int, value: Int): Unit = run { baseByteBuffer.putInt(index * 4, value) }
	actual fun getFloat(index: Int): Float = baseByteBuffer.getFloat(index * 4)
	actual fun setFloat(index: Int, value: Float): Unit = run { baseByteBuffer.putFloat(index * 4, value) }
	actual fun dispose() = Unit
}

@Suppress("USELESS_CAST")
val KmlNativeBuffer.nioBuffer: ByteBuffer
	get() = (this as KmlNativeBuffer).byteBuffer
@Suppress("USELESS_CAST")
val KmlNativeBuffer.nioByteBuffer: ByteBuffer
	get() = (this as KmlNativeBuffer).byteBuffer
@Suppress("USELESS_CAST")
val KmlNativeBuffer.nioShortBuffer: ShortBuffer
	get() = (this as KmlNativeBuffer).shortBuffer
@Suppress("USELESS_CAST")
val KmlNativeBuffer.nioIntBuffer: IntBuffer
	get() = (this as KmlNativeBuffer).intBuffer
@Suppress("USELESS_CAST")
val KmlNativeBuffer.nioFloatBuffer: FloatBuffer
	get() = (this as KmlNativeBuffer).floatBuffer
