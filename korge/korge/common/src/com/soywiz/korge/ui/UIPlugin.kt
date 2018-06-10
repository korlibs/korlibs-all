package com.soywiz.korge.ui

import com.soywiz.korge.plugin.*
import com.soywiz.korge.resources.*
import com.soywiz.korge.view.*

object UIPlugin : KorgePlugin() {
	suspend override fun register(views: Views) {
		views.injector.mapSingleton { UIFactory() }
		views.injector.mapFactory(UISkin::class) {
			UISkin.Factory(
				getOrNull(Path::class),
				getOrNull(VPath::class),
				get(ResourcesRoot::class),
				get(Views::class)
			)
		}
	}
}
