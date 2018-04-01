package com.soywiz.kmedialayer

fun KmlNativeBuffer.toAsciiString(): String {
    var out = ""
    for (n in 0 until size) {
        val b = getByte(n)
        if (b == 0.toByte()) break
        out += b.toChar()
    }
    return out
}

fun KmlNativeBuffer.putAsciiString(str: String): KmlNativeBuffer {
    var n = 0
    for (c in str) {
        if (size >= n) setByte(n++, c.toByte())
    }
    if (size >= n) setByte(n++, 0.toByte())
    return this
}

fun kmlByteBufferOf(vararg values: Byte) = KmlNativeBuffer(values.size * 1).apply { for (n in 0 until values.size) this.setByte(n, values[n]) }
fun kmlShortBufferOf(vararg values: Short) = KmlNativeBuffer(values.size * 2).apply { for (n in 0 until values.size) this.setShort(n, values[n]) }
fun kmlIntBufferOf(vararg values: Int) = KmlNativeBuffer(values.size * 4).apply { for (n in 0 until values.size) this.setInt(n, values[n]) }
fun kmlFloatBufferOf(vararg values: Float) = KmlNativeBuffer(values.size * 4).apply { for (n in 0 until values.size) this.setFloat(n, values[n]) }

inline fun <T> kmlNativeBuffer(size: Int, callback: (KmlNativeBuffer) -> T): T {
    val buffer = KmlNativeBuffer(size)
    try {
        return callback(buffer)
    } finally {
        buffer.dispose()
    }
}

inline fun <T> kmlNativeIntBuffer(size: Int, callback: (KmlNativeBuffer) -> T): T = kmlNativeBuffer(size * 4, callback)

fun <T> IntArray.toTempBuffer(callback: (KmlNativeBuffer) -> T): T {
    return kmlNativeIntBuffer(this.size) { buffer ->
        for (n in this.indices) buffer.setInt(n, this[n])
        callback(buffer)
    }
}