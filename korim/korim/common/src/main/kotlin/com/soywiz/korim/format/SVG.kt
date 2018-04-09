package com.soywiz.korim.format

import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.math.*

object SVG : ImageFormat("svg") {
	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
		val start = s.slice().readString(min(100, s.length.toInt())).trim().toLowerCase()
		try {
			if (start.startsWith("<svg") || start.startsWith("<?xml")) {
				val content = s.slice().readAll().toString(UTF8).trim()
				val svg = com.soywiz.korim.vector.format.SVG(content)
				return ImageInfo().apply {
					width = svg.width
					height = svg.height
				}
			} else {
				return null
			}
		} catch (t: Throwable) {
			return null
		}
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
		val content = s.slice().readAll().toString(UTF8).trim()
		val svg = com.soywiz.korim.vector.format.SVG(content)
		return ImageData(
			listOf(
				ImageFrame(
					svg.render().toBmp32()
				)
			)
		)
	}
}