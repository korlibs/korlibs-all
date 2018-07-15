package com.soywiz.korge.lipsync

import com.soywiz.korge.view.*

fun Views.registerLipsync() {
	println("LipsyncPlugin.register()")
	views.registerPropertyTrigger("lipsync") { view, key, value ->
		view.addComponent(LipSyncComponent(view))
	}
}