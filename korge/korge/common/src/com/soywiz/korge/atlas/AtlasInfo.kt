package com.soywiz.korge.atlas

import com.soywiz.korge.util.*
import com.soywiz.korio.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korma.geom.*

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(AtlasInfo.Factory::class)
data class AtlasInfo(
	val frames: Map<String, Entry>,
	val meta: Meta
) {
	data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
		val rect get() = Rectangle(x, y, w, h)
	}

	data class Size(val w: Int, val h: Int) {
		val size get() = com.soywiz.korma.geom.Size(w, h)
	}

	data class Meta(
		val app: String,
		val format: String,
		val image: String,
		val scale: Double,
		val size: Size,
		val version: String
	)

	data class Entry(
		val frame: Rect,
		val rotated: Boolean,
		val sourceSize: Size,
		val spriteSourceSize: Rect,
		val trimmed: Boolean
	) {
		fun applyRotation() = if (rotated) {
			this.copy(
				frame = frame.copy(w = frame.h, h = frame.w),
				spriteSourceSize = spriteSourceSize.copy(
					x = spriteSourceSize.y,
					y = spriteSourceSize.x,
					w = spriteSourceSize.h,
					h = spriteSourceSize.w
				)
			)
		} else {
			this
		}
	}

	val app: String get() = meta.app
	val format: String get() = meta.format
	val image: String get() = meta.image
	val scale: Double get() = meta.scale
	val size: Size get() = meta.size
	val version: String get() = meta.version

	companion object {
		init {
			Mapper.registerType(Rect::class) {
				Rect(
					x = it["x"].gen(),
					y = it["y"].gen(),
					w = it["w"].gen(),
					h = it["h"].gen()
				)
			}

			Mapper.registerType(Size::class) {
				Size(
					w = it["w"].gen(),
					h = it["h"].gen()
				)
			}

			Mapper.registerType(Entry::class) {
				Entry(
					frame = it["frame"].gen(),
					rotated = Dynamic.toBool2(it["rotated"]),
					sourceSize = it["sourceSize"].gen(),
					spriteSourceSize = it["spriteSourceSize"].gen(),
					trimmed = Dynamic.toBool2(it["trimmed"])
				)
			}

			Mapper.registerType(Meta::class) {
				Meta(
					app = it["app"].gen(),
					format = it["format"].gen(),
					image = it["image"].gen(),
					scale = it["scale"].gen(),
					size = it["size"].gen(),
					version = it["version"].gen()
				)
			}

			Mapper.registerType(AtlasInfo::class) {
				AtlasInfo(
					frames = it["frames"].genMap(),
					meta = it["meta"].gen()
				)
			}
		}

		fun loadJsonSpriter(@Language("json") json: String): AtlasInfo {
			val info = Json.decodeToType(AtlasInfo::class, json)
			return info.copy(frames = info.frames.mapValues { it.value.applyRotation() })
		}
	}
}
