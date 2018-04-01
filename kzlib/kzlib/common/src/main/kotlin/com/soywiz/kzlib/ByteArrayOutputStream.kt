package com.soywiz.kzlib

open class ByteArrayOutputStream(size: Int = 32) : OutputStream() {
	protected var buf: ByteArray = ByteArray(size)
	protected var count: Int = 0

	private fun expand(i: Int) {
		if (count + i <= buf.size) return
		val newbuf = ByteArray((count + i) * 2)
		arraycopy(buf, 0, newbuf, 0, count)
		buf = newbuf
	}

	open fun reset() = run { count = 0 }
	fun size(): Int = count
	fun toByteArray(): ByteArray {
		val newArray = ByteArray(count)
		arraycopy(buf, 0, newArray, 0, count)
		return newArray
	}

	override fun toString(): String = buf.toSimpleString()

	override fun write(buffer: ByteArray, offset: Int, len: Int) {
		if (len == 0) return
		expand(len)
		arraycopy(buffer, offset, buf, this.count, len)
		this.count += len
	}

	override fun write(oneByte: Int) {
		if (count == buf.size) expand(1)
		buf[count++] = oneByte.toByte()
	}

	fun writeTo(out: OutputStream) = run { out.write(buf, 0, count) }
}
