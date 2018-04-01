package com.soywiz.kmedialayer.scene.components

import com.soywiz.kmedialayer.*
import com.soywiz.kmedialayer.scene.*
import com.soywiz.kmedialayer.scene.util.*

interface KeyEvents {
    val up: Signal<Key>
    val down: Signal<Key>
    fun up(key: Key, callback: View.() -> Unit)
    fun down(key: Key, callback: View.() -> Unit)
}

class KeyEventsComponent(override val view: View) : KeyComponent, KeyEvents {
    override val up = Signal<Key>()
    override val down = Signal<Key>()

    override fun up(key: Key, callback: View.() -> Unit): Unit = run { up { if (it == key) callback(view) } }
    override fun down(key: Key, callback: View.() -> Unit): Unit = run { down { if (it == key) callback(view) } }

    override fun onKeyUp(key: Key) = up(key)
    override fun onKeyDown(key: Key) = down(key)
}

operator fun KeyEvents.invoke(callback: KeyEvents.() -> Unit) = this.apply(callback)
val View.keys get() = getOrCreateComponent { KeyEventsComponent(it) } as KeyEvents
