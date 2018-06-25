package com.soywiz.korge.atlas

import com.soywiz.korge.plugin.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object AtlasPlugin : KorgePlugin() {
	init {
		Mapper.registerType {
			AtlasInfo.Meta(
				it["app"].gen(),
				it["format"].gen(),
				it["image"].gen(),
				it["scale"].gen(),
				it["size"].gen(),
				it["version"].gen()
			)
		}
		Mapper.registerType { AtlasInfo.Rect(it["x"].gen(), it["y"].gen(), it["w"].gen(), it["h"].gen()) }
		Mapper.registerType { AtlasInfo.Size(it["w"].gen(), it["h"].gen()) }
		Mapper.registerType {
			AtlasInfo.Entry(
				it["frame"].gen(),
				it["rotated"].gen(),
				it["source"].gen(),
				it["spriteSourceSize"].gen(),
				it["trimmed"].gen()
			)
		}
		Mapper.registerType { AtlasInfo(it["frames"].genMap(), it["meta"].gen()) }
	}

	override suspend fun register(views: Views) {
	}
}

class Atlas(val info: AtlasInfo) {
	val textures = hashMapOf<String, TransformedTexture>()

	operator fun get(name: String) = textures[name] ?: invalidOp("Can't find texture '$name' in atlas")

	internal suspend fun load(views: Views, folder: VfsFile): Atlas = this.apply {
		val atlasTex = folder[info.image].readTexture(views)
		for ((frameName, frame) in info.frames) {
			textures[frameName] = TransformedTexture(
				atlasTex.slice(frame.frame.rect),
				frame.spriteSourceSize.x.toFloat(), frame.spriteSourceSize.y.toFloat(),
				frame.rotated
			)
		}
	}
}

suspend fun VfsFile.readAtlas(views: Views): Atlas {
	return Atlas(AtlasInfo.loadJsonSpriter(this.readString())).load(views, this.parent)
}
