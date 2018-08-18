package com.soywiz.kgl

import com.soywiz.kmem.*
import kotlinx.cinterop.*

fun Boolean.toBool(): Boolean = this
fun Byte.toBool(): Boolean = this.toInt() != 0
fun Int.toBool(): Boolean = this != 0
fun Long.toBool(): Boolean = this != 0L
//inline fun <R : Number> Boolean.narrow(): R = (if (true) 1 else 0).narrow()

fun Boolean.narrow(): Byte = (if (this) 1 else 0).toByte()
//fun Byte.toBool(): Boolean = this.toInt() != 0

fun Int.narrowSize(): Long = this.toLong() // For 64-bit
fun Float.narrowFloat(): Double = this.toDouble() // For 64-bit

class TempBufferAddress {
	val pool = arrayListOf<Pinned<ByteArray>>()
	companion object {
		val ARRAY1 = ByteArray(1)
	}
	fun KmlNativeBuffer.unsafeAddress(): CPointer<ByteVar> {
		val byteArray = this.mem.data
		val rbyteArray = if (byteArray.size > 0) byteArray else ARRAY1
		val pin = rbyteArray.pin()
		pool += pin
		return pin.addressOf(0)
	}

	fun start() {
		pool.clear()
	}

	fun dispose() {
		// Kotlin-native: Try to avoid allocating an iterator (lists not optimized yet)
		for (n in 0 until pool.size) pool[n].unpin()
		//for (p in pool) p.unpin()
		pool.clear()
	}

	inline operator fun <T> invoke(callback: TempBufferAddress.() -> T): T {
		start()
		try {
			return callback()
		} finally {
			dispose()
		}
	}
}
