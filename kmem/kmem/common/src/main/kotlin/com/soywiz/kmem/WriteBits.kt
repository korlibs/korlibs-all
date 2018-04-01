package com.soywiz.kmem

fun ByteArray.write8(o: Int, v: Int) = run { this[o] = v.toByte() }
fun ByteArray.write8(o: Int, v: Long) = run { this[o] = v.toByte() }

fun ByteArray.write16_LEBE(o: Int, v: Int, little: Boolean) = if (little) write16_le(o, v) else write16_be(o, v)
fun ByteArray.write32_LEBE(o: Int, v: Int, little: Boolean) = if (little) write32_le(o, v) else write32_be(o, v)
fun ByteArray.write64_LEBE(o: Int, v: Long, little: Boolean) = if (little) write64_le(o, v) else write64_be(o, v)

fun ByteArray.writeF32_LEBE(o: Int, v: Float, little: Boolean) = if (little) writeF32_le(o, v) else writeF32_be(o, v)
fun ByteArray.writeF64_LEBE(o: Int, v: Double, little: Boolean) = if (little) writeF64_le(o, v) else writeF64_be(o, v)

fun ByteArray.write16_le(o: Int, v: Int) = run { this[o + 0] = v.extract8(0).toByte(); this[o + 1] = v.extract8(8).toByte() }
fun ByteArray.write24_le(o: Int, v: Int) = run { this[o + 0] = v.extract8(0).toByte(); this[o + 1] = v.extract8(8).toByte(); this[o + 2] = v.extract8(16).toByte() }
fun ByteArray.write32_le(o: Int, v: Int) = run { this[o + 0] = v.extract8(0).toByte(); this[o + 1] = v.extract8(8).toByte(); this[o + 2] = v.extract8(16).toByte(); this[o + 3] = v.extract8(24).toByte() }
fun ByteArray.write32_le(o: Int, v: Long) = write32_le(o, v.toInt())
fun ByteArray.write64_le(o: Int, v: Long) = run { write32_le(o + 0, (v ushr 0).toInt()); write32_le(o + 4, (v ushr 32).toInt()) }

fun ByteArray.writeF32_le(o: Int, v: Float) = run { write32_le(o + 0, v.toRawBits()) }
fun ByteArray.writeF64_le(o: Int, v: Double) = run { write64_le(o + 0, v.toRawBits()) }

fun ByteArray.write16_be(o: Int, v: Int) = run { this[o + 1] = v.extract8(0).toByte(); this[o + 0] = v.extract8(8).toByte() }
fun ByteArray.write24_be(o: Int, v: Int) = run { this[o + 2] = v.extract8(0).toByte(); this[o + 1] = v.extract8(8).toByte(); this[o + 0] = v.extract8(16).toByte() }
fun ByteArray.write32_be(o: Int, v: Int) = run { this[o + 3] = v.extract8(0).toByte(); this[o + 2] = v.extract8(8).toByte(); this[o + 1] = v.extract8(16).toByte(); this[o + 0] = v.extract8(24).toByte() }
fun ByteArray.write32_be(o: Int, v: Long) = write32_be(o, v.toInt())
fun ByteArray.write64_be(o: Int, v: Long) = run { write32_le(o + 0, (v ushr 32).toInt()); write32_le(o + 4, (v ushr 0).toInt()) }

fun ByteArray.writeF32_be(o: Int, v: Float) = run { write32_be(o + 0, v.toRawBits()) }
fun ByteArray.writeF64_be(o: Int, v: Double) = run { write64_be(o + 0, v.toRawBits()) }

fun ByteArray.writeBytes(o: Int, bytes: ByteArray) = arraycopy(bytes, 0, this, o, bytes.size)
fun ByteArray.writeBytes(o: Int, bytes: UByteArray) = arraycopy(bytes.data, 0, this, o, bytes.size)

private inline fun writeTypedArray(o: Int, elementSize: Int, indices: IntRange, write: (p: Int, n: Int) -> Unit) {
	var p = o
	for (n in indices) {
		write(p, n)
		p += elementSize
	}
}

fun ByteArray.writeArray_le(o: Int, array: CharArray) = writeTypedArray(o, 2, array.indices) { p, n -> write16_le(p, array[n].toInt()) }
fun ByteArray.writeArray_le(o: Int, array: ShortArray) = writeTypedArray(o, 2, array.indices) { p, n -> write16_le(p, array[n].toInt()) }
fun ByteArray.writeArray_le(o: Int, array: IntArray) = writeTypedArray(o, 4, array.indices) { p, n -> write32_le(p, array[n]) }
fun ByteArray.writeArray_le(o: Int, array: LongArray) = writeTypedArray(o, 8, array.indices) { p, n -> write64_le(p, array[n]) }
fun ByteArray.writeArray_le(o: Int, array: FloatArray) = writeTypedArray(o, 4, array.indices) { p, n -> writeF32_le(p, array[n]) }
fun ByteArray.writeArray_le(o: Int, array: DoubleArray) = writeTypedArray(o, 8, array.indices) { p, n -> writeF64_le(p, array[n]) }

fun ByteArray.writeArray_be(o: Int, array: CharArray) = writeTypedArray(o, 2, array.indices) { p, n -> write16_be(p, array[n].toInt()) }
fun ByteArray.writeArray_be(o: Int, array: ShortArray) = writeTypedArray(o, 2, array.indices) { p, n -> write16_be(p, array[n].toInt()) }
fun ByteArray.writeArray_be(o: Int, array: IntArray) = writeTypedArray(o, 4, array.indices) { p, n -> write32_be(p, array[n]) }
fun ByteArray.writeArray_be(o: Int, array: LongArray) = writeTypedArray(o, 8, array.indices) { p, n -> write64_be(p, array[n]) }
fun ByteArray.writeArray_be(o: Int, array: FloatArray) = writeTypedArray(o, 4, array.indices) { p, n -> writeF32_be(p, array[n]) }
fun ByteArray.writeArray_be(o: Int, array: DoubleArray) = writeTypedArray(o, 8, array.indices) { p, n -> writeF64_be(p, array[n]) }
