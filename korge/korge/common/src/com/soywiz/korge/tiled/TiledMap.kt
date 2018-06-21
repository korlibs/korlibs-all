package com.soywiz.korge.tiled

import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korge.render.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.resources.Path
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import kotlin.collections.set
import kotlin.coroutines.experimental.*
import kotlin.math.*

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(TiledMapFactory::class)
class TiledMap {
	var width = 0
	var height = 0
	var tilewidth = 0
	var tileheight = 0
	val pixelWidth: Int get() = width * tilewidth
	val pixelHeight: Int get() = height * tileheight
	val tilesets = arrayListOf<TiledTileset>()
	val allLayers = arrayListOf<Layer>()
	lateinit var tileset: TileSet
	inline val patternLayers get() = allLayers.patterns
	inline val imageLayers get() = allLayers.images
	inline val objectLayers get() = allLayers.objects

	class TiledTileset(val tileset: TileSet, val firstgid: Int = 0) {
	}

	sealed class Layer {
		var name: String = ""
		var visible: Boolean = true
		var draworder: String = ""
		var color: Int = -1
		var opacity = 1.0
		var offsetx: Double = 0.0
		var offsety: Double = 0.0
		val properties = hashMapOf<String, Any>()

		class Patterns : Layer() {
			//val tilemap = TileMap(Bitmap32(0, 0), )
			var map: Bitmap32 = Bitmap32(0, 0)
		}

		class Objects : Layer() {
			open class Object(val bounds: IRectangleInt)
			open class Poly(bounds: IRectangleInt, val points: List<Point2d>) : Object(bounds)
			class Rect(bounds: IRectangleInt) : Object(bounds)
			class Ellipse(bounds: IRectangleInt) : Object(bounds)
			class Polyline(bounds: IRectangleInt, points: List<Point2d>) : Poly(bounds, points)
			class Polygon(bounds: IRectangleInt, points: List<Point2d>) : Poly(bounds, points)

			val objects = arrayListOf<Object>()
		}

		class Image : Layer() {
			var image: Bitmap = Bitmap32(0, 0)
		}
	}
}

inline val Iterable<TiledMap.Layer>.patterns get() = this.filterIsInstance<TiledMap.Layer.Patterns>()
inline val Iterable<TiledMap.Layer>.images get() = this.filterIsInstance<TiledMap.Layer.Image>()
inline val Iterable<TiledMap.Layer>.objects get() = this.filterIsInstance<TiledMap.Layer.Objects>()

private val spaces = Regex("\\s+")

val tilemapLog = Logger("tilemap")

suspend fun VfsFile.readTiledMap(
	views: Views,
	hasTransparentColor: Boolean = false,
	transparentColor: Int = Colors.FUCHSIA,
	createBorder: Int = 1
): TiledMap {
	val log = tilemapLog
	val file = this
	val folder = this.parent.jail()
	val tiledMap = TiledMap()
	val mapXml = file.readXml()

	if (mapXml.nameLC != "map") error("Not a TiledMap XML TMX file starting with <map>")

	tiledMap.width = mapXml.getInt("width") ?: 0
	tiledMap.height = mapXml.getInt("height") ?: 0
	tiledMap.tilewidth = mapXml.getInt("tilewidth") ?: 32
	tiledMap.tileheight = mapXml.getInt("tileheight") ?: 32

	tilemapLog.trace { "tilemap: width=${tiledMap.width}, height=${tiledMap.height}, tilewidth=${tiledMap.tilewidth}, tileheight=${tiledMap.tileheight}" }
	tilemapLog.trace { "tilemap: $tiledMap" }

	val elements = mapXml.allChildrenNoComments

	tilemapLog.trace { "tilemap: elements=${elements.size}" }
	tilemapLog.trace { "tilemap: elements=$elements" }

	var maxGid = 1
	//var lastBaseTexture = views.transparentTexture.base

	for (element in elements) {
		val elementName = element.nameLC
		@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
		when {
			elementName == "tileset" -> {
				tilemapLog.trace { "tileset" }
				val firstgid = element.int("firstgid")

				// TSX file
				val element = if (element.hasAttribute("source")) {
					folder[element.str("source")].readXml()
				} else {
					element
				}

				val name = element.str("name")
				val tilewidth = element.int("tilewidth")
				val tileheight = element.int("tileheight")
				val tilecount = element.int("tilecount", -1)
				val columns = element.int("columns", -1)
				val image = element.child("image")
				val source = image?.str("source") ?: ""
				val width = image?.int("width", 0) ?: 0
				val height = image?.int("height", 0) ?: 0
				var bmp = folder[source].readBitmapOptimized()
				//var bmp = folder[source].readBitmap()

				// @TODO: Preprocess this, so in JS we don't have to do anything!
				if (hasTransparentColor) {
					bmp = bmp.toBMP32()
					for (n in 0 until bmp.area) {
						if (bmp.data[n] == transparentColor) bmp.data[n] = Colors.TRANSPARENT_BLACK
					}
				}

				val tileset = if (createBorder > 0) {
					bmp = bmp.toBMP32()

					val slices = TileSet.extractBitmaps(bmp, tilewidth, tileheight, columns, tilecount)

					TileSet.fromBitmaps(
						views,
						tilewidth, tileheight,
						slices,
						border = createBorder,
						mipmaps = false
					)
				} else {
					val tex = views.texture(bmp, mipmaps = true)
					TileSet(views, Texture(tex.base), tilewidth, tileheight, columns, tilecount)
				}

				val tiledTileset = TiledMap.TiledTileset(
					tileset = tileset,
					firstgid = firstgid
				)

				//lastBaseTexture = tex.base
				tilemapLog.trace { "tileset:$tiledTileset" }
				tiledMap.tilesets += tiledTileset
				maxGid = max(maxGid, firstgid + tiledTileset.tileset.textures.size)
			}
			elementName == "layer" || elementName == "objectgroup" || elementName == "imagelayer" -> {
				tilemapLog.trace { "layer:$elementName" }
				val layer = when (element.nameLC) {
					"layer" -> TiledMap.Layer.Patterns()
					"objectgroup" -> TiledMap.Layer.Objects()
					"imagelayer" -> TiledMap.Layer.Image()
					else -> invalidOp
				}
				tiledMap.allLayers += layer
				layer.name = element.str("name")
				layer.visible = element.int("visible", 1) != 0
				layer.draworder = element.str("draworder", "")
				layer.color = NamedColors[element.str("color", "#ffffff")]
				layer.opacity = element.double("opacity", 1.0)
				layer.offsetx = element.double("offsetx", 0.0)
				layer.offsety = element.double("offsety", 0.0)

				val propertiesXml = element.child("properties")
				if (propertiesXml != null) {
					for (property in propertiesXml.children("property")) {
						val pname = property.str("name")
						val rawValue = property.str("rawValue")
						val type = property.str("type", "text")
						val pvalue: Any = when (type) {
							"bool" -> rawValue == "true"
							"color" -> NamedColors[rawValue]
							"text" -> rawValue
							"int" -> rawValue.toIntOrNull() ?: 0
							"float" -> rawValue.toDoubleOrNull() ?: 0.0
							"file" -> folder[pname]
							else -> rawValue
						}
						layer.properties[pname] = pvalue
						//println("$pname: $pvalue")
					}
				}

				when (layer) {
					is TiledMap.Layer.Patterns -> {
						val width = element.int("width")
						val height = element.int("height")
						val count = width * height
						val data = element.child("data")
						val encoding = data?.str("encoding", "") ?: ""
						val compression = data?.str("compression", "") ?: ""
						@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
						val tilesArray: IntArray = when {
							encoding == "" || encoding == "xml" -> {
								val items = data?.children("tile")?.map { it.int("gid") } ?: listOf()
								items.toIntArray()
							}
							encoding == "csv" -> {
								val content = data?.text ?: ""
								val items = content.replace(spaces, "").split(',').map(String::toInt)
								items.toIntArray()
							}
							encoding == "base64" -> {
								val base64Content = (data?.text ?: "").trim()
								val rawContent = Base64.decode(base64Content)

								val content = when (compression) {
									"" -> rawContent
									"gzip" -> rawContent.uncompress(GZIP)
									"zlib" -> rawContent.uncompress(ZLib)
									else -> invalidOp
								}
								content.readIntArray_le(0, count)
							}
							else -> invalidOp("Unhandled encoding '$encoding'")
						}
						if (tilesArray.size != count) invalidOp("")
						layer.map = Bitmap32(width, height, tilesArray)
					}
					is TiledMap.Layer.Image -> {
						for (image in element.children("image")) {
							val source = image.str("source")
							val width = image.int("width")
							val height = image.int("height")
							layer.image = folder[source].readBitmapOptimized()
						}
					}
					is TiledMap.Layer.Objects -> {
						for (obj in element.children("object")) {
							val x = obj.int("x")
							val y = obj.int("y")
							val width = obj.int("width")
							val height = obj.int("height")
							val bounds = IRectangleInt(x, y, width, height)
							val kind = obj.allNodeChildren.firstOrNull()
							val kindType = kind?.nameLC ?: ""
							@Suppress("IntroduceWhenSubject") // @TODO: BUG IN KOTLIN-JS with multicase in suspend functions
							layer.objects += when {
								kindType == "" -> TiledMap.Layer.Objects.Rect(bounds)
								kindType == "ellipse" -> TiledMap.Layer.Objects.Ellipse(bounds)
								kindType == "polyline" || kindType == "polygon" -> {
									val pointsStr = kind!!.str("points")
									val points = pointsStr.split(spaces).map {
										val parts = it.split(',').map { it.trim().toDoubleOrNull() ?: 0.0 }
										Point2d(parts[0], parts[1])
									}

									if (kindType == "polyline") {
										TiledMap.Layer.Objects.Polyline(bounds, points)
									} else {
										TiledMap.Layer.Objects.Polygon(bounds, points)
									}
								}
								else -> invalidOp("Invalid object kind $kindType")
							}
						}
					}
				}
			}
		}
	}

	val combinedTileset = kotlin.arrayOfNulls<Texture>(maxGid + 1)

	for (tileset in tiledMap.tilesets) {
		for (n in 0 until tileset.tileset.textures.size) {
			combinedTileset[tileset.firstgid + n] = tileset.tileset.textures[n]
		}
	}

	tiledMap.tileset = TileSet(views, combinedTileset.toList(), tiledMap.tilewidth, tiledMap.tileheight)

	return tiledMap
}

class TiledMapFactory(
	val views: Views,
	val resourcesRoot: ResourcesRoot,
	val path: Path
) : AsyncFactory<TiledMap> {
	suspend override fun create(): TiledMap = resourcesRoot[path].readTiledMap(views)
}
