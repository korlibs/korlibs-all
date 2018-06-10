package com.soywiz.korge.component

import com.soywiz.korge.event.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import kotlin.coroutines.experimental.*

open class Component(val view: View) : CoroutineContextHolder {
	override val coroutineContext: CoroutineContext get() = view.views.coroutineContext
	val detatchCancellables = arrayListOf<Cancellable>()

	val views: Views get() = view.views
	fun attach() = view.addComponent(this)
	fun dettach() = view.removeComponent(this)
	fun afterDetatch() {
		for (e in detatchCancellables) e.cancel()
		detatchCancellables.clear()
	}

	open fun update(dtMs: Int): Unit = Unit

	inline fun <reified T : Any> addEventListener(noinline handler: (T) -> Unit) {
		detatchCancellables += this.view.addEventListener<T>(handler)
	}
}
