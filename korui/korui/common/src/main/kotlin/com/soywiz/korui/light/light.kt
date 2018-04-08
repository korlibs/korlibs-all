package com.soywiz.korui.light

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.vfs.*

expect object NativeLightsComponentsFactory : LightComponentsFactory {
	override fun create(): LightComponents
}

interface LightComponentsFactory {
	fun create(): LightComponents
}

open class LightComponents {
	class LightComponentInfo(val handle: Any) : Extra by Extra.Mixin()

	open fun create(type: LightType): LightComponentInfo = LightComponentInfo(Unit)
	open fun setParent(c: Any, parent: Any?): Unit = Unit

	var insideEventHandler: Boolean = false; private set

	protected fun <T> insideEventHandler(callback: () -> T): T {
		val oldEventHandler = this.insideEventHandler
		try {
			this.insideEventHandler = true
			return callback()
		} finally {
			this.insideEventHandler = oldEventHandler
		}
	}

	protected fun LightMouseHandler.up2(info: LightMouseHandler.Info) = insideEventHandler { this.up(info) }
	protected fun LightMouseHandler.down2(info: LightMouseHandler.Info) = insideEventHandler { this.down(info) }
	protected fun LightMouseHandler.click2(info: LightMouseHandler.Info) = insideEventHandler { this.click(info) }
	protected fun LightMouseHandler.over2(info: LightMouseHandler.Info) = insideEventHandler { this.over(info) }
	protected fun LightMouseHandler.enter2(info: LightMouseHandler.Info) = insideEventHandler { this.enter(info) }
	protected fun LightMouseHandler.exit2(info: LightMouseHandler.Info) = insideEventHandler { this.exit(info) }
	protected fun LightChangeHandler.changed2(info: LightChangeHandler.Info) = insideEventHandler { this.changed(info) }
	protected fun LightResizeHandler.resized2(info: LightResizeHandler.Info) = insideEventHandler { this.resized(info) }
	protected fun LightKeyHandler.up2(info: LightKeyHandler.Info) = insideEventHandler { this.up(info) }
	protected fun LightKeyHandler.down2(info: LightKeyHandler.Info) = insideEventHandler { this.down(info) }
	protected fun LightKeyHandler.typed2(info: LightKeyHandler.Info) = insideEventHandler { this.typed(info) }
	protected fun LightTouchHandler.start2(info: LightTouchHandler.Info) = insideEventHandler { this.start(info) }
	protected fun LightTouchHandler.end2(info: LightTouchHandler.Info) = insideEventHandler { this.end(info) }
	protected fun LightTouchHandler.move2(info: LightTouchHandler.Info) = insideEventHandler { this.move(info) }

	open fun addHandler(c: Any, listener: LightMouseHandler): Closeable = Closeable { }
	open fun addHandler(c: Any, listener: LightChangeHandler): Closeable = Closeable { }
	open fun addHandler(c: Any, listener: LightResizeHandler): Closeable = Closeable { }
	open fun addHandler(c: Any, listener: LightKeyHandler): Closeable = Closeable { }
	open fun addHandler(c: Any, listener: LightGamepadHandler): Closeable = Closeable { }
	open fun addHandler(c: Any, listener: LightTouchHandler): Closeable = Closeable { }
	open fun addHandler(c: Any, listener: LightDropHandler): Closeable = Closeable { }

	open fun getDpi(): Double = 96.0
	open fun <T> callAction(c: Any, key: LightAction<T>, param: T): Unit = Unit
	open fun <T> setProperty(c: Any, key: LightProperty<T>, value: T): Unit = Unit
	open fun <T> getProperty(c: Any, key: LightProperty<T>): T = key.default
	open fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int): Unit = Unit
	open fun repaint(c: Any): Unit = Unit
	open suspend fun dialogAlert(c: Any, message: String): Unit = Unit
	open suspend fun dialogPrompt(c: Any, message: String, initialValue: String = ""): String =
		throw UnsupportedOperationException()

	open suspend fun dialogOpenFile(c: Any, filter: String): VfsFile = throw UnsupportedOperationException()
	open fun openURL(url: String): Unit = Unit
	open fun open(file: VfsFile): Unit = openURL(file.absolutePath)
}

open class LightChangeHandler {
	data class Info(var dummy: Boolean = true)

	open fun changed(info: Info) = Unit
}

open class LightResizeHandler {
	data class Info(var width: Int = 0, var height: Int = 0)

	open fun resized(info: Info) = Unit
}

open class LightMouseHandler {
	data class Info(
		var x: Int = 0,
		var y: Int = 0,
		var buttons: Int = 0,
		var isShiftDown: Boolean = false,
		var isCtrlDown: Boolean = false,
		var isAltDown: Boolean = false,
		var isMetaDown: Boolean = false
	)

	open fun enter(info: Info) = Unit
	open fun exit(info: Info) = Unit
	open fun over(info: Info) = Unit
	open fun up(info: Info) = Unit
	open fun down(info: Info) = Unit
	open fun click(info: Info) = Unit
}

open class LightDropHandler {
	data class FileInfo(
		val files: List<VfsFile>
	)

	class EnterInfo()

	open fun enter(info: EnterInfo): Boolean = true
	open fun exit(): Unit = Unit
	open fun files(info: FileInfo) = Unit
}

open class LightKeyHandler {
	data class Info(
		var keyCode: Int = 0
	)

	open fun typed(info: Info) = Unit
	open fun down(info: Info) = Unit
	open fun up(info: Info) = Unit
}

open class LightGamepadHandler {
	data class Info(
		var gamepad: GamepadInfo = GamepadInfo()
	)

	open fun update(info: Info) = Unit
}

open class LightTouchHandler {
	data class Info(
		var x: Int = 0,
		var y: Int = 0,
		var id: Int = 0
	)

	open fun start(info: Info) = Unit
	open fun end(info: Info) = Unit
	open fun move(info: Info) = Unit
}

val defaultLightFactory: LightComponentsFactory by lazy { NativeLightsComponentsFactory }
val defaultLight: LightComponents by lazy { defaultLightFactory.create() }

enum class LightType {
	FRAME, CONTAINER, BUTTON, PROGRESS, IMAGE, LABEL, TEXT_FIELD, TEXT_AREA, CHECK_BOX, SCROLL_PANE, AGCANVAS
}

class LightAction<T>(val name: String) {
	companion object {
		val FOCUS = LightAction<Any?>("FOCUS")
	}

	@Suppress("UNCHECKED_CAST")
	operator fun get(v: Any?): T = v as T
}

class LightProperty<out T>(val name: String, val default: T) {
	companion object {
		val VISIBLE = LightProperty<Boolean>("VISIBLE", true)
		val TEXT = LightProperty<String>("TEXT", "")
		val ICON = LightProperty<Bitmap?>("ICON", null)
		val BGCOLOR = LightProperty<Int>("BGCOLOR", Colors.BLACK)
		val PROGRESS_CURRENT = LightProperty<Int>("PROGRESS_CURRENT", 0)
		val PROGRESS_MAX = LightProperty<Int>("PROGRESS_MAX", 100)
		val IMAGE = LightProperty<Bitmap?>("IMAGE", null)
		val IMAGE_SMOOTH = LightProperty<Boolean>("IMAGE_SMOOTH", true)
		val CHECKED = LightProperty<Boolean>("CHECKED", false)
	}

	@Suppress("UNCHECKED_CAST")
	operator fun get(v: Any?): T = v as T

	@Suppress("UNCHECKED_CAST")
	fun getOrDefault(v: Any?): T = if (v == null) default else v as T

	override fun toString(): String = "LightProperty[$name]"
}

var LightComponents.LightComponentInfo.ag: AG? by extraProperty("ag", null)
