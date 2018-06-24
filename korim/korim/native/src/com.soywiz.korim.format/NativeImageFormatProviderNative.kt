package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.coroutine.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import com.soywiz.korma.*
import kotlin.math.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = NativeNativeImageFormatProvider

object NativeNativeImageFormatProvider : NativeImageFormatProvider() {
	override suspend fun decode(data: ByteArray): NativeImage = TODO()
	override suspend fun decode(vfs: Vfs, path: String): NativeImage = TODO()
	override fun create(width: Int, height: Int): NativeImage = TODO()
	override fun copy(bmp: Bitmap): NativeImage = TODO()
	override suspend fun display(bitmap: Bitmap, kind: Int) = TODO()
	override fun mipmap(bmp: Bitmap, levels: Int): NativeImage = TODO()
	override fun mipmap(bmp: Bitmap): NativeImage = TODO()
}
