package com.soywiz.korge.lipsync

import com.soywiz.korge.plugin.*
import com.soywiz.korge.view.*

object LipsyncPlugin : KorgePlugin() {
	suspend override fun register(views: Views) {
		println("LipsyncPlugin.register()")
		views.registerPropertyTrigger("lipsync") { view, key, value ->
			view.addComponent(LipSyncComponent(view))
		}
	}
}
