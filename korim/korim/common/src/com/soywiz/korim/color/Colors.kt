package com.soywiz.korim.color

import com.soywiz.korio.lang.*

object Colors {
	val WHITE = RGBA.packFast(0xFF, 0xFF, 0xFF, 0xFF)
	val BLACK = RGBA.packFast(0x00, 0x00, 0x00, 0xFF)
	val RED = RGBA.packFast(0xFF, 0x00, 0x00, 0xFF)
	val GREEN = RGBA.packFast(0x00, 0xFF, 0x00, 0xFF)
	val BLUE = RGBA.packFast(0x00, 0x00, 0xFF, 0xFF)
	val LIME = RGBA(0, 0xFF, 0, 0xFF)
	val ORANGE = RGBA(0xFF, 0xA5, 0x00, 0xFF)
	val PINK = RGBA(0xFF, 0xC0, 0xCB, 0xFF)
	val FUCHSIA = RGBA(0xFF, 0x00, 0xFF, 0xFF)

	val TRANSPARENT_BLACK = RGBA.packFast(0x00, 0x00, 0x00, 0x00)
	val TRANSPARENT_WHITE = RGBA.packFast(0x00, 0x00, 0x00, 0x00)

	val colorsByName by lazy {
		mapOf(
			"black" to BLACK,
			"white" to WHITE,
			"red" to RED,
			"green" to GREEN,
			"blue" to BLUE,
			"lime" to LIME,
			"orange" to ORANGE,
			"pink" to PINK
		)
	}

	operator fun get(str: String): Int = get(str, 0, errorOnDefault = true)

	operator fun get(str: String, default: Int, errorOnDefault: Boolean = false): Int {
		when {
			str.startsWith("#") -> {
				val hex = str.substr(1)
				if (hex.length !in setOf(3, 4, 6, 8)) return BLACK
				val chars = if (hex.length < 6) 1 else 2
				val scale = if (hex.length < 6) (255.0 / 15.0) else 1.0
				val hasAlpha = (hex.length / chars) >= 4
				val r = (hex.substr(0 * chars, chars).toInt(0x10) * scale).toInt()
				val g = (hex.substr(1 * chars, chars).toInt(0x10) * scale).toInt()
				val b = (hex.substr(2 * chars, chars).toInt(0x10) * scale).toInt()
				val a = if (hasAlpha) (hex.substr(3 * chars, chars).toInt(0x10) * scale).toInt() else 0xFF
				return RGBA.pack(r, g, b, a)
			}
			str.startsWith("RGBA(", ignoreCase = true) -> {
				val parts = str.toUpperCase().removePrefix("RGBA(").removeSuffix(")").split(",")
				val r = parts.getOrElse(0) { "0" }.toIntOrNull() ?: 0
				val g = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
				val b = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
				val af = parts.getOrElse(3) { "1.0" }.toDoubleOrNull() ?: 1.0
				return RGBA(r, g, b, (af * 255).toInt())
			}
			else -> {
				val col = colorsByName[str.toLowerCase()]
				if (col == null && errorOnDefault) error("Unsupported color '$str'")
				return col ?: default
			}
		}
	}

	fun toHtmlString(color: Int) =
		"RGBA(" + RGBA.getR(color) + "," + RGBA.getG(color) + "," + RGBA.getB(color) + "," + RGBA.getAf(color) + ")"

	fun toHtmlStringSimple(color: Int) = "#%02x%02x%02x".format(RGBA.getR(color), RGBA.getG(color), RGBA.getB(color))
}