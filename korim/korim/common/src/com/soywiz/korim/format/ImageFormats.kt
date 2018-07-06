package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

class ImageFormats(formats: Iterable<ImageFormat>) : ImageFormat("") {
	val formats = formats.toSet()
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		for (format in formats) return try {
			format.decodeHeader(s.slice(), props) ?: continue
		} catch (e: Throwable) {
			continue
		}
		return null
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val format = formats.firstOrNull { it.check(s.slice(), props) }
		if (format != null) return format.readImage(s.slice(), props)
		throw UnsupportedOperationException(
			"Not suitable image format : MAGIC:" + s.slice().readString(
				4,
				ASCII
			) + "(" + s.sliceStart().readBytes(4).hex + ") (" + s.sliceStart().readBytes(4).toString(ASCII) + ")"
		)
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val ext = PathInfo(props.filename).extensionLC
		//println("filename: $filename")
		val format = formats.firstOrNull { ext in it.extensions }
				?: throw UnsupportedOperationException("Don't know how to generate file for extension '$ext' (supported extensions ${formats.flatMap { it.extensions }}) (props $props)")
		format.writeImage(image, s, props)
	}
}

suspend fun Bitmap.writeTo(
	file: VfsFile,
	props: ImageEncodingProps = ImageEncodingProps(),
	formats: ImageFormat = defaultImageFormats
) = file.writeBytes(formats.encode(this, props.copy(filename = file.basename)))

val defaultImageFormats = ImageFormats(StandardImageFormats.toSet())
//val defaultImageFormats = ImageFormats()
