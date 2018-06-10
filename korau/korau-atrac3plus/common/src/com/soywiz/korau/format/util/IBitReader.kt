package com.soywiz.korau.format.util

interface IBitReader {
	fun read1(): Int
	fun readBool(): Boolean
	fun read(n: Int): Int
	fun peek(n: Int): Int
	fun skip(n: Int)
}
