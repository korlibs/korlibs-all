package com.soywiz.kmedialayer


interface Charset {
    fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int)
    fun encode(out: ByteArrayOutputStream, src: CharSequence, start: Int, end: Int)
}

object UTF8 : Charset {
    private fun createByte(codePoint: Int, shift: Int): Int = codePoint shr shift and 0x3F or 0x80

    override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
        var i = start
        while (i < end) {
            val c = src[i++].toInt() and 0xFF
            when (c shr 4) {
                0, 1, 2, 3, 4, 5, 6, 7 -> {
                    // 0xxxxxxx
                    out.append(c.toChar())
                }
                12, 13 -> {
                    // 110x xxxx   10xx xxxx
                    out.append((c and 0x1F shl 6 or (src[i++].toInt() and 0x3F)).toChar())
                }
                14 -> {
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    out.append((c and 0x0F shl 12 or (src[i++].toInt() and 0x3F shl 6) or (src[i++].toInt() and 0x3F)).toChar())
                }
            }
        }
    }

    override fun encode(out: ByteArrayOutputStream, src: CharSequence, start: Int, end: Int) {
        for (n in start until end) {
            val codePoint = src[n].toInt()

            if (codePoint and 0x7F.inv() == 0) { // 1-byte sequence
                out.u8(codePoint)
            } else {
                if (codePoint and 0x7FF.inv() == 0) { // 2-byte sequence
                    out.u8((codePoint shr 6 and 0x1F or 0xC0))
                } else if (codePoint and 0xFFFF.inv() == 0) { // 3-byte sequence
                    out.u8((codePoint shr 12 and 0x0F or 0xE0))
                    out.u8((createByte(codePoint, 6)))
                } else if (codePoint and -0x200000 == 0) { // 4-byte sequence
                    out.u8((codePoint shr 18 and 0x07 or 0xF0))
                    out.u8((createByte(codePoint, 12)))
                    out.u8((createByte(codePoint, 6)))
                }
                out.u8((codePoint and 0x3F or 0x80))
            }
        }
    }
}

object ASCII : Charset {
    override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
        for (n in start until end) out.append(src[n].toChar())
    }

    override fun encode(out: ByteArrayOutputStream, src: CharSequence, start: Int, end: Int) {
        for (n in start until end) out.u8(src[n].toInt())
    }
}

fun ByteArray.toString(charset: Charset): String =
    StringBuilder().apply { charset.decode(this, this@toString, 0, this@toString.size) }.toString()

fun String.toByteArray(charset: Charset): ByteArray =
    buildByteArray { charset.encode(this, this@toByteArray, 0, this@toByteArray.length) }
