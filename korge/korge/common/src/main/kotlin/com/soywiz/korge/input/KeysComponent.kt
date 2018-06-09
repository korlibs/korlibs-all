package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.event.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*

class KeysComponent(view: View) : Component(view) {
	val onKeyDown = AsyncSignal<KeyDownEvent>()
	val onKeyUp = AsyncSignal<KeyUpEvent>()
	val onKeyTyped = AsyncSignal<KeyTypedEvent>()

	fun down(keyCode: Int, callback: (keyCode: Int) -> Unit): Closeable =
		onKeyDown { e -> if (e.keyCode == keyCode) callback(keyCode) }

	fun up(keyCode: Int, callback: (keyCode: Int) -> Unit): Closeable =
		onKeyUp { e -> if (e.keyCode == keyCode) callback(keyCode) }

	fun typed(keyCode: Int, callback: (keyCode: Int) -> Unit): Closeable =
		onKeyTyped { e -> if (e.keyCode == keyCode) callback(keyCode) }

	init {
		this.detatchCancellables += view.addEventListener<KeyDownEvent> { go { onKeyDown(it) } }
		this.detatchCancellables += view.addEventListener<KeyUpEvent> { go { onKeyUp(it) } }
		this.detatchCancellables += view.addEventListener<KeyTypedEvent> { go { onKeyTyped(it) } }
	}
}

val View.keys by Extra.PropertyThis<View, KeysComponent> { this.getOrCreateComponent { KeysComponent(this) } }

inline fun <T : View?> T?.onKeyDown(noinline handler: suspend (KeyDownEvent) -> Unit) =
	this.apply { this?.keys?.onKeyDown?.add(handler) }

inline fun <T : View?> T?.onKeyUp(noinline handler: suspend (KeyUpEvent) -> Unit) =
	this.apply { this?.keys?.onKeyUp?.add(handler) }

inline fun <T : View?> T?.onKeyTyped(noinline handler: suspend (KeyTypedEvent) -> Unit) =
	this.apply { this?.keys?.onKeyTyped?.add(handler) }
