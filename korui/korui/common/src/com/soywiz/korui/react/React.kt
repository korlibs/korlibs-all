package com.soywiz.korui.react

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korui.*
import com.soywiz.korui.ui.*

object React

// @TODO: Virtual DOM and reuse components
abstract class ReactComponent<TState : Any>() {
	private lateinit var root: Container

	private lateinit var _state: TState

	var state: TState
		get() = _state
		set(value) {
			_state = value
			root.removeAll()
			launchImmediately(KorioDefaultDispatcher) {
				root.render()
				root.relayout()
			}
		}

	abstract suspend fun Container.render()

	fun React.attach(root: Container, initialState: TState) {
		this@ReactComponent.root = root
		this@ReactComponent.state = initialState
	}
}

fun <TState : Any> Container.attachReactComponent(component: ReactComponent<TState>, state: TState) {
	component.apply {
		React.attach(this@attachReactComponent, state)
	}
}

suspend fun <TState : Any> Application.reactFrame(
	component: ReactComponent<TState>,
	state: TState,
	title: String,
	width: Int = 640,
	height: Int = 480,
	icon: Bitmap? = null
): Frame {
	return frame(title, width, height, icon) {
		attachReactComponent(component, state)
	}
}
