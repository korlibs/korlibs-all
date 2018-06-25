package com.soywiz.korge.ui

import com.soywiz.korge.plugin.*
import com.soywiz.korge.view.*

object UIPlugin : KorgePlugin() {
	override suspend fun register(views: Views) {
		views.injector.mapSingleton { UIFactory() }
	}
}
