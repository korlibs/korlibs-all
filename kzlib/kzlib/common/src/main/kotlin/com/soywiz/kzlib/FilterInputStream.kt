package com.soywiz.kzlib

open class FilterInputStream protected constructor(protected var i: InputStream) : InputStream() {
	override fun read(): Int = i.read()
	override fun read(b: ByteArray): Int = read(b, 0, b.size)
	override fun read(b: ByteArray, off: Int, len: Int): Int = i.read(b, off, len)
	override fun skip(n: Long): Long = i.skip(n)
	override fun available(): Int = i.available()
	override fun close() = run { i.close() }
	override fun mark(readlimit: Int) = run { i.mark(readlimit) }
	override fun reset() = run { i.reset() }
	override fun markSupported(): Boolean = i.markSupported()
}
