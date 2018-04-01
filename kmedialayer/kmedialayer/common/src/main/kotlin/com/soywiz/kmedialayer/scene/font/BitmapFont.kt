package com.soywiz.kmedialayer.scene.font

import com.soywiz.kmedialayer.*
import com.soywiz.kmedialayer.scene.*
import com.soywiz.kmedialayer.scene.geom.*

class BitmapFont(val texture: SceneTexture, val lineHeight: Int, val base: Int, val chars: List<BmpChar>, val kernings: List<Kerning>) {
    data class Kerning(val first: Int, val second: Int, val amount: Int)
    data class BmpChar(
        val id: Int, val x: Int, val y: Int, val width: Int, val height: Int,
        val xoffset: Int, var yoffset: Int, val xadvance: Int,
        val page: Int, val chnl: Int
    )

    data class Glyph(val bmpChar: BmpChar, val texture: SceneTexture, val kernings: Map<Int, Kerning>) {
        val id get() = bmpChar.id
    }

    val kerningsByFirst = kernings.groupBy { it.first }
    val glyphsById = chars.map {
        val tex = texture.sliceSize(it.x, it.y, it.width, it.height)
        Glyph(it, tex, (kerningsByFirst[it.id] ?: listOf<Kerning>()).associateBy { it.second })
    }.associateBy { it.id }

    operator fun get(id: Int) = glyphsById[id] ?: glyphsById.values.first()
    operator fun get(id: Char) = get(id.toInt())
}

suspend fun Scene.readBitmapFont(name: String): BitmapFont {
    val kernings = arrayListOf<BitmapFont.Kerning>()
    val chars = arrayListOf<BitmapFont.BmpChar>()
    var imageName = "$name.png"
    var lineHeight = 16
    var base: Int? = null
    for (rline in kml.loadFileString(name).lines()) {
        val line = rline.trim()
        val map = LinkedHashMap<String, String>()
        for (part in line.split(' ')) {
            val (key, value) = part.split('=') + listOf("", "")
            map[key] = value
        }
        when {
            line.startsWith("info") -> {
            }
            line.startsWith("page") -> {
                imageName = map["file"]?.trim('"') ?: "$name.png"
            }
            line.startsWith("common ") -> {
                lineHeight = map["lineHeight"]?.toIntOrNull() ?: 16
                base = map["base"]?.toIntOrNull()
            }
            line.startsWith("char ") -> {
                //id=54 x=158 y=88 width=28 height=42 xoffset=2 yoffset=8 xadvance=28 page=0 chnl=0
                chars += BitmapFont.BmpChar(
                    id = map["id"]?.toIntOrNull() ?: 0,
                    x = map["x"]?.toIntOrNull() ?: 0,
                    y = map["y"]?.toIntOrNull() ?: 0,
                    width = map["width"]?.toIntOrNull() ?: 0,
                    height = map["height"]?.toIntOrNull() ?: 0,
                    xoffset = map["xoffset"]?.toIntOrNull() ?: 0,
                    yoffset = map["yoffset"]?.toIntOrNull() ?: 0,
                    xadvance = map["xadvance"]?.toIntOrNull() ?: 0,
                    page = map["page"]?.toIntOrNull() ?: 0,
                    chnl = map["chnl"]?.toIntOrNull() ?: 0
                )
            }
            line.startsWith("kerning ") -> {
                kernings += BitmapFont.Kerning(
                    first = map["first"]?.toIntOrNull() ?: 0,
                    second = map["second"]?.toIntOrNull() ?: 0,
                    amount = map["amount"]?.toIntOrNull() ?: 0
                )
            }
        }
    }
    val texture = texture(imageName)
    return BitmapFont(texture, lineHeight, base ?: lineHeight, chars, kernings)
}

fun BitmapFont.renderQuads(gm: Matrix2d, text: String, ssx: Double, ssy: Double, localBounds: Rectangle? = Rectangle(), out: ArrayList<QuadWithTexture>? = arrayListOf()): ArrayList<QuadWithTexture>? {
    val font = this
    var sx = ssx
    var sy = ssy

    for (n in 0 until text.length) {
        val c = text[n]
        when (c) {
            '\n' -> {
                sx = ssx
                sy += font.lineHeight.toDouble()
            }
            else -> {
                val glyph = font[c]
                val quad = Quad()
                val c = glyph.bmpChar
                if (out != null) {
                    quad.set(gm, sx + c.xoffset, sy + c.yoffset, c.width.toDouble(), c.height.toDouble())
                    out += QuadWithTexture(quad, glyph.texture)
                }
                sx += c.xadvance
            }
        }
    }
    localBounds?.setTo(ssx, ssy, sx - ssx, sy - ssy + font.lineHeight)
    return out
}
