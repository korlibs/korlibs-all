package com.soywiz.korge.component

import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korui.event.*
import kotlin.coroutines.experimental.*
import kotlin.reflect.*

open class Component(val view: View) : CoroutineContextHolder, EventDispatcher by view {
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

	override fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Cancellable {
		val c = this.view.addEventListener<T>(clazz, handler)
		detatchCancellables += c
		return Cancellable { detatchCancellables -= c }
	}

	override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
		this.view.dispatch(clazz, event)
	}
}
