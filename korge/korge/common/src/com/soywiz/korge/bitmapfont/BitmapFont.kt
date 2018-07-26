package com.soywiz.korge.bitmapfont

import com.soywiz.kds.*
import com.soywiz.korge.html.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.util.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.*

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(BitmapFontAsyncFactory::class)
class BitmapFont(
	val fontSize: Int,
	val glyphs: IntMap<Glyph>,
	val kernings: IntMap<Kerning>
) : Html.MetricsProvider {
	override fun getBounds(text: String, format: Html.Format, out: Rectangle) {
		//val font = getBitmapFont(format.computedFace, format.computedSize)
		val font = this
		val scale = format.computedSize.toDouble() / font.fontSize.toDouble()
		var width = 0.0
		var height = 0.0
		var dy = 0.0
		var dx = 0.0
		for (n in 0 until text.length) {
			val c1 = text[n].toInt()
			if (c1 == '\n'.toInt()) {
				dx = 0.0
				dy += font.fontSize
				height = max(height, dy)
				continue
			}
			val c2 = text.getOrElse(n + 1) { ' ' }.toInt()
			val kerningOffset = font.kernings[BitmapFont.Kerning.buildKey(c1, c2)]?.amount ?: 0
			val glyph = font[c1]
			dx += glyph.xadvance + kerningOffset
			width = max(width, dx)
		}
		height += font.fontSize
		out.setTo(0.0, 0.0, width * scale, height * scale)
	}

	constructor(fontSize: Int, glyphs: Map<Int, Glyph>, kernings: Map<Int, Kerning>) : this(
		fontSize, glyphs.toIntMap(), kernings.toIntMap()
	)

	class Kerning(
		val first: Int,
		val second: Int,
		val amount: Int
	) {
		companion object {
			fun buildKey(f: Int, s: Int) = f or (s shl 16)
		}
	}

	class Glyph(
		val id: Int,
		val texture: BitmapSlice<Bitmap>,
		val xoffset: Int,
		val yoffset: Int,
		val xadvance: Int
	)

	val dummyGlyph by lazy { Glyph(-1, Bitmaps.transparent, 0, 0, 0) }

	operator fun get(charCode: Int): Glyph = glyphs[charCode] ?: glyphs[32] ?: dummyGlyph
	operator fun get(char: Char): Glyph = this[char.toInt()]

	fun drawText(
		ctx: RenderContext,
		textSize: Double,
		str: String,
		x: Int,
		y: Int,
		m: Matrix2d = Matrix2d(),
		colMul: RGBA = Colors.WHITE,
		colAdd: Int = 0x7f7f7f7f,
		blendMode: BlendMode = BlendMode.INHERIT,
		filtering: Boolean = true
	) {
		val m2 = m.clone()
		val scale = textSize / fontSize.toDouble()
		m2.pretranslate(x.toDouble(), y.toDouble())
		m2.prescale(scale, scale)
		var dx = 0
		var dy = 0
		for (n in str.indices) {
			val c1 = str[n].toInt()
			if (c1 == '\n'.toInt()) {
				dx = 0
				dy += fontSize
				continue
			}
			val c2 = str.getOrElse(n + 1) { ' ' }.toInt()
			val glyph = this[c1]
			val tex = glyph.texture
			ctx.batch.drawQuad(
				ctx.getTex(tex),
				(dx + glyph.xoffset).toFloat(),
				(dy + glyph.yoffset).toFloat(),
				m = m2,
				colorMul = colMul,
				colorAdd = colAdd,
				blendFactors = blendMode.factors,
				filtering = filtering
			)
			val kerningOffset = kernings[Kerning.buildKey(c1, c2)]?.amount ?: 0
			dx += glyph.xadvance + kerningOffset
		}
	}

	companion object {
		operator fun invoke(
			fontName: String,
			fontSize: Int,
			chars: String = BitmapFontGenerator.LATIN_ALL,
			mipmaps: Boolean = true
		): BitmapFont {
			return BitmapFontGenerator.generate(fontName, fontSize, chars).convert()
		}
	}
}

fun RenderContext.drawText(
	font: BitmapFont,
	textSize: Double,
	str: String,
	x: Int,
	y: Int,
	m: Matrix2d = Matrix2d(),
	colMul: RGBA = Colors.WHITE,
	colAdd: Int = 0x7f7f7f7f,
	blendMode: BlendMode = BlendMode.INHERIT
) {
	font.drawText(this, textSize, str, x, y, m, colMul, colAdd, blendMode)
}

suspend fun VfsFile.readBitmapFont(imageFormats: ImageFormats = defaultImageFormats): BitmapFont {
	val fntFile = this
	val content = fntFile.readString().trim()
	val textures = hashMapOf<Int, BitmapSlice<Bitmap>>()

	when {
	// XML
		content.startsWith('<') -> {
			val xml = Xml(content)

			val fontSize = xml["info"].firstOrNull()?.int("size", 16) ?: 16

			for (page in xml["pages"]["page"]) {
				val id = page.int("id")
				val file = page.str("file")
				val texFile = fntFile.parent[file]
				val tex = texFile.readBitmapSlice()
				textures[id] = tex
			}

			val glyphs = xml["chars"]["char"].map {
				val page = it.int("page")
				val texture = textures[page] ?: textures.values.first()
				BitmapFont.Glyph(
					id = it.int("id"),
					texture = texture.sliceWithSize(it.int("x"), it.int("y"), it.int("width"), it.int("height")),
					xoffset = it.int("xoffset"),
					yoffset = it.int("yoffset"),
					xadvance = it.int("xadvance")
				)
			}

			val kernings = xml["kernings"]["kerning"].map {
				BitmapFont.Kerning(
					first = it.int("first"),
					second = it.int("second"),
					amount = it.int("amount")
				)
			}

			return BitmapFont(
				fontSize = fontSize,
				glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
				kernings = kernings.map { BitmapFont.Kerning.buildKey(it.first, it.second) to it }.toMap().toIntMap()
			)
		}
	// FNT
		content.startsWith("info") -> {
			data class BmpChar(
				val id: Int, val x: Int, val y: Int, val width: Int, val height: Int,
				val xoffset: Int, var yoffset: Int, val xadvance: Int,
				val page: Int, val chnl: Int
			)

			val kernings = arrayListOf<BitmapFont.Kerning>()
			val glyphs = arrayListOf<BitmapFont.Glyph>()
			var lineHeight = 16
			var base: Int? = null
			for (rline in content.lines()) {
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
						val id = map["id"]?.toInt() ?: 0
						val file = map["file"]?.unquote() ?: error("page without file")
						textures[id] = fntFile.parent[file].readBitmapSlice()
					}
					line.startsWith("common ") -> {
						lineHeight = map["lineHeight"]?.toIntOrNull() ?: 16
						base = map["base"]?.toIntOrNull()
					}
					line.startsWith("char ") -> {
						//id=54 x=158 y=88 width=28 height=42 xoffset=2 yoffset=8 xadvance=28 page=0 chnl=0
						val page = map["page"]?.toIntOrNull() ?: 0
						val texture = textures[page] ?: textures.values.first()
						glyphs += Dynamic {
							BitmapFont.Glyph(
								id = map["id"].int,
								xoffset = map["xoffset"].int,
								yoffset = map["yoffset"].int,
								xadvance = map["xadvance"].int,
								texture = texture.sliceWithSize(map["x"].int, map["y"].int, map["width"].int, map["height"].int)
							)
						}
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
			return BitmapFont(
				fontSize = lineHeight,
				glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
				kernings = kernings.map { BitmapFont.Kerning.buildKey(it.first, it.second) to it }.toMap().toIntMap()
			)
		}
		else -> TODO("Unsupported font type starting with ${content.substr(0, 16)}")
	}
}

// @TODO: Move to kds
fun <T> Map<Int, T>.toIntMap(): IntMap<T> {
	val out = IntMap<T>()
	for ((k, v) in this) out[k] = v
	return out
}

fun com.soywiz.korim.font.BitmapFont.toKorge(): BitmapFont = convert()

fun com.soywiz.korim.font.BitmapFont.convert(): BitmapFont {
	val font = this

	val mipmaps = true
	val atlasBitmap = if (mipmaps) font.atlas.ensurePowerOfTwo() else font.atlas

	val tex = atlasBitmap
	val glyphs = arrayListOf<BitmapFont.Glyph>()
	for (info in font.glyphInfos) {
		val bounds = info.bounds
		val texSlice = tex.sliceWithSize(bounds.x, bounds.y, bounds.width, bounds.height)
		glyphs += BitmapFont.Glyph(info.id, texSlice, 0, 0, info.advance)
	}
	return BitmapFont(font.size, glyphs.map { it.id to it }.toMap().toIntMap(), IntMap())
}
