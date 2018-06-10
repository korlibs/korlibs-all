package com.soywiz.korge.input

import com.soywiz.kds.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korui.event.*
import com.soywiz.korui.input.*

class KeysComponent(view: View) : Component(view) {
	val onKeyDown = AsyncSignal<KeyEvent>()
	val onKeyUp = AsyncSignal<KeyEvent>()
	val onKeyTyped = AsyncSignal<KeyEvent>()

	fun down(key: Key, callback: (key: Key) -> Unit): Closeable =
		onKeyDown { e -> if (e.key == key) callback(key) }

	fun up(key: Key, callback: (key: Key) -> Unit): Closeable =
		onKeyUp { e -> if (e.key == key) callback(key) }

	fun typed(key: Key, callback: (key: Key) -> Unit): Closeable =
		onKeyTyped { e -> if (e.key == key) callback(key) }

	init {
		keys {
			detatchCancellables += this.down { go { onKeyDown(this) } }
			detatchCancellables += this.up { go { onKeyUp(this) } }
			detatchCancellables += this.press { go { onKeyTyped(this) } }
		}
	}
}

val View.keys by Extra.PropertyThis<View, KeysComponent> { this.getOrCreateComponent { KeysComponent(this) } }
inline fun <T> View.keys(callback: KeysComponent.() -> T): T = keys.run(callback)

inline fun <T : View?> T?.onKeyDown(noinline handler: suspend (KeyEvent) -> Unit) =
	this.apply { this?.keys?.onKeyDown?.add(handler) }

inline fun <T : View?> T?.onKeyUp(noinline handler: suspend (KeyEvent) -> Unit) =
	this.apply { this?.keys?.onKeyUp?.add(handler) }

inline fun <T : View?> T?.onKeyTyped(noinline handler: suspend (KeyEvent) -> Unit) =
	this.apply { this?.keys?.onKeyTyped?.add(handler) }