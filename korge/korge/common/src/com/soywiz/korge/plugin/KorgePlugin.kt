package com.soywiz.korge.plugin

import com.soywiz.korge.atlas.*
import com.soywiz.korge.audio.*
import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.view.*
import com.soywiz.korinject.*

abstract class KorgePlugin {
	abstract suspend fun register(views: Views): Unit
}

val defaultKorgePlugins = KorgePlugins().apply {
	register(AtlasPlugin, SoundPlugin)
}

@Singleton
open class KorgePlugins {
	val plugins = LinkedHashSet<KorgePlugin>()

	fun register(vararg plugins: KorgePlugin) = this.apply { this@KorgePlugins.plugins += plugins }
}
