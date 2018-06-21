package com.soywiz.korge.bitmapfont

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korge.plugin.*
import com.soywiz.korge.render.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.resources.Path
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korinject.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.*
import kotlin.collections.Map
import kotlin.collections.arrayListOf
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.first
import kotlin.collections.firstOrNull
import kotlin.collections.hashMapOf
import kotlin.collections.iterator
import kotlin.collections.map
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.collections.toMap

object BitmapFontPlugin : KorgePlugin() {
	override suspend fun register(views: Views) {
		views.injector
			.mapFactory {
				BitmapFontAsyncFactory(
					getOrNull(Path::class),
					getOrNull(VPath::class),
					getOrNull(FontDescriptor::class),
					get(ResourcesRoot::class),
					get(AG::class)
				)
			}
	}
}

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(BitmapFontAsyncFactory::class)
class BitmapFont(
	val ag: AG,
	val fontSize: Int,
	val glyphs: IntMap<Glyph>,
	val kernings: IntMap<Kerning>
) {
	constructor(ag: AG, fontSize: Int, glyphs: Map<Int, Glyph>, kernings: Map<Int, Kerning>) : this(
		ag, fontSize, glyphs.toIntMap(), kernings.toIntMap()
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
		val texture: Texture,
		val xoffset: Int,
		val yoffset: Int,
		val xadvance: Int
	)

	val dummyGlyph by lazy { Glyph(-1, Texture(ag.dummyTexture, 1, 1), 0, 0, 0) }

	operator fun get(charCode: Int): Glyph = glyphs[charCode] ?: glyphs[32] ?: dummyGlyph
	operator fun get(char: Char): Glyph = this[char.toInt()]

	fun drawText(
		batch: BatchBuilder2D,
		textSize: Double,
		str: String,
		x: Int,
		y: Int,
		m: Matrix2d = Matrix2d(),
		colMul: Int = Colors.WHITE,
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
			batch.drawQuad(
				tex,
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
			ag: AG,
			fontName: String,
			fontSize: Int,
			chars: String = BitmapFontGenerator.LATIN_ALL,
			mipmaps: Boolean = true
		): BitmapFont {
			return BitmapFontGenerator.generate(fontName, fontSize, chars).convert(ag, mipmaps = mipmaps)
		}
	}
}

fun BatchBuilder2D.drawText(
	font: BitmapFont,
	textSize: Double,
	str: String,
	x: Int,
	y: Int,
	m: Matrix2d = Matrix2d(),
	colMul: Int = Colors.WHITE,
	colAdd: Int = 0x7f7f7f7f,
	blendMode: BlendMode = BlendMode.INHERIT
) {
	font.drawText(this, textSize, str, x, y, m, colMul, colAdd, blendMode)
}

suspend fun VfsFile.readBitmapFont(ag: AG): BitmapFont {
	val fntFile = this
	val content = fntFile.readString().trim()
	val textures = hashMapOf<Int, Texture>()

	when {
	// XML
		content.startsWith('<') -> {
			val xml = Xml(content)

			val fontSize = xml["info"].firstOrNull()?.int("size", 16) ?: 16

			for (page in xml["pages"]["page"]) {
				val id = page.int("id")
				val file = page.str("file")
				val texFile = fntFile.parent[file]
				val tex = texFile.readTexture(ag)
				textures[id] = tex
			}

			val glyphs = xml["chars"]["char"].map {
				val page = it.int("page")
				val texture = textures[page] ?: textures.values.first()
				BitmapFont.Glyph(
					id = it.int("id"),
					texture = texture.slice(it.int("x"), it.int("y"), it.int("width"), it.int("height")),
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
				ag = ag,
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
						textures[id] = fntFile.parent[file].readTexture(ag)
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
								texture = texture.slice(map["x"].int, map["y"].int, map["width"].int, map["height"].int)
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
				ag = ag,
				fontSize = lineHeight,
				glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
				kernings = kernings.map { BitmapFont.Kerning.buildKey(it.first, it.second) to it }.toMap().toIntMap()
			)
		}
		else ->TODO("Unsupported font type starting with ${content.substr(0, 16)}")
	}
}

// @TODO: Move to kds
fun <T> Map<Int, T>.toIntMap(): IntMap<T> {
	val out = IntMap<T>()
	for ((k, v) in this) out.set(k, v)
	return out
}

annotation class FontDescriptor(val face: String, val size: Int, val chars: String = "0123456789")

class BitmapFontAsyncFactory(
	@Optional private val path: Path?,
	@Optional private val vpath: VPath?,
	@Optional private val descriptor: FontDescriptor?,
	private val resourcesRoot: ResourcesRoot,
	private val ag: AG
) : AsyncFactory<BitmapFont> {
	override suspend fun create() = if (path != null) {
		resourcesRoot[path].readBitmapFont(ag)
	} else if (vpath != null) {
		resourcesRoot[vpath.path].readBitmapFont(ag)
	} else if (descriptor != null) {
		com.soywiz.korim.font.BitmapFontGenerator.generate(descriptor.face, descriptor.size, descriptor.chars)
			.convert(ag)
	} else {
		invalidOp("BitmapFont injection requires @Path or @FontDescriptor annotations")
	}
}

fun com.soywiz.korim.font.BitmapFont.toKorge(views: Views, mipmaps: Boolean = true): BitmapFont =
	convert(views.ag, mipmaps)

fun com.soywiz.korim.font.BitmapFont.convert(ag: AG, mipmaps: Boolean = true): BitmapFont {
	val font = this

	val atlasBitmap = if (mipmaps) font.atlas.ensurePowerOfTwo() else font.atlas

	val tex = Texture(ag.createTexture().upload(atlasBitmap, mipmaps), atlasBitmap.width, atlasBitmap.height)
	val glyphs = arrayListOf<BitmapFont.Glyph>()
	for (info in font.glyphInfos) {
		val bounds = info.bounds
		val texSlice = tex.slice(bounds.x, bounds.y, bounds.width, bounds.height)
		glyphs += BitmapFont.Glyph(info.id, texSlice, 0, 0, info.advance)
	}
	return BitmapFont(ag, font.size, glyphs.map { it.id to it }.toMap().toIntMap(), IntMap())
}
