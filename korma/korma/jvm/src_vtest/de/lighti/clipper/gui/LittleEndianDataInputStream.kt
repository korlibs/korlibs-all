package de.lighti.clipper.gui

import com.soywiz.korio.util.toUnsigned
import java.io.DataInput
import java.io.EOFException
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Works like DataInputStream but reverses the bits (big/little endian) when reading
 * values

 * @author Tobias Mahlmann
 */
class LittleEndianDataInputStream(`in`: InputStream) : FilterInputStream(`in`), DataInput {
	private val w: ByteArray

	init {

		w = ByteArray(8)
	}

	@Throws(IOException::class)
	override fun readBoolean(): Boolean {
		val ch = `in`.read()
		if (ch < 0) {
			throw EOFException()
		}
		return ch != 0
	}

	@Throws(IOException::class)
	override fun readByte(): Byte {
		val ch = `in`.read()
		if (ch < 0) {
			throw EOFException()
		}
		return ch.toByte()
	}

	/**
	 * like DataInputStream.readChar except little endian.
	 */
	@Throws(IOException::class)
	override fun readChar(): Char {
		readFully(w, 0, 2)
		return (w[1].toInt().toUnsigned() shl 8 or (w[0].toInt().toUnsigned())).toChar()
	}

	@Throws(IOException::class)
	override fun readDouble(): Double {
		return java.lang.Double.longBitsToDouble(readLong())
	}

	@Throws(IOException::class)
	override fun readFloat(): Float {
		return java.lang.Float.intBitsToFloat(readInt())
	}

	@Throws(IOException::class)
	override fun readFully(b: ByteArray) {
		readFully(b, 0, b.size)
	}

	@Throws(IOException::class)
	override fun readFully(b: ByteArray, off: Int, len: Int) {
		if (len < 0) {
			throw IndexOutOfBoundsException()
		}
		var n = 0
		while (n < len) {
			val count = `in`.read(b, off + n, len - n)
			if (count < 0) {
				throw EOFException()
			}
			n += count
		}
	}

	/**
	 * like DataInputStream.readInt except little endian.
	 */
	@Throws(IOException::class)
	override fun readInt(): Int {
		readFully(w, 0, 4)
		return w[3].toUnsigned() shl 24 or (w[2].toUnsigned() shl 16) or (w[1].toUnsigned() shl 8) or (w[0].toUnsigned())
	}

	@Throws(IOException::class)
	override fun readLine(): String {
		throw UnsupportedOperationException()
	}

	/**
	 * like DataInputStream.readLong except little endian.
	 */
	@Throws(IOException::class)
	override fun readLong(): Long {
		readFully(w, 0, 8)
		return w[7].toLong() shl 56 or ((w[6].toUnsigned()).toLong() shl 48) or ((w[5].toUnsigned()).toLong() shl 40) or ((w[4].toUnsigned()).toLong() shl 32) or ((w[3].toUnsigned()).toLong() shl 24) or ((w[2].toUnsigned()).toLong() shl 16) or ((w[1].toUnsigned()).toLong() shl 8) or (w[0].toUnsigned()).toLong()
	}

	@Throws(IOException::class)
	override fun readShort(): Short {
		readFully(w, 0, 2)
		return (w[1].toUnsigned() shl 8 or (w[0].toUnsigned())).toShort()
	}

	@Throws(IOException::class)
	override fun readUnsignedByte(): Int {
		val ch = `in`.read()
		if (ch < 0) {
			throw EOFException()
		}
		return ch
	}

	@Throws(IOException::class)
	override fun readUnsignedShort(): Int {
		readFully(w, 0, 2)
		return w[1].toUnsigned() shl 8 or (w[0].toUnsigned())
	}

	@Throws(IOException::class)
	override fun readUTF(): String {
		throw UnsupportedOperationException()
	}

	@Throws(IOException::class)
	override fun skipBytes(n: Int): Int {
		var total = 0
		var cur = 0

		while (total < n) {
			cur = `in`.skip((n - total).toLong()).toInt()
			if (cur <= 0) break
			total += cur
		}

		return total
	}

}