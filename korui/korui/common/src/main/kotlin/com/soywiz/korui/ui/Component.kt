@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "NOTHING_TO_INLINE")

package com.soywiz.korui.ui

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.vfs.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.light.*
import com.soywiz.korui.style.*
import kotlin.reflect.*

open class Component(val app: Application, val type: LightType) : Styled, Extra by Extra.Mixin() {
	val coroutineContext = app.coroutineContext
	val lc = app.light

	class lightProperty<T>(
		val key: LightProperty<T>,
		val getable: Boolean = false,
		val setHandler: ((v: T) -> Unit)? = null
	) {
		inline operator fun getValue(thisRef: Component, property: KProperty<*>): T {
			if (getable) return thisRef.lc.getProperty(thisRef.handle, key)
			return thisRef.getProperty(key)
		}

		inline operator fun setValue(thisRef: Component, property: KProperty<*>, value: T): Unit = run {
			thisRef.setProperty(key, value)
			setHandler?.invoke(value)
		}
	}

	override var style = Style()
	val componentInfo = lc.create(type)
	var handle: Any = componentInfo.handle
	val properties = LinkedHashMap<LightProperty<*>, Any?>()
	var valid = false
	protected var nativeBounds = RectangleInt()
	val actualBounds: RectangleInt = RectangleInt()

	val actualWidth: Int get() = actualBounds.width
	val actualHeight: Int get() = actualBounds.height

	fun <T> setProperty(key: LightProperty<T>, value: T, reset: Boolean = false) {
		if (reset || (properties[key] != value)) {
			properties[key] = value
			lc.setProperty(handle, key, value)
		}
	}

	fun <T> getProperty(key: LightProperty<T>): T = if (key in properties) properties[key] as T else key.default

	fun setBoundsInternal(bounds: RectangleInt) = setBoundsInternal(bounds.x, bounds.y, bounds.width, bounds.height)

	fun setBoundsInternal(x: Int, y: Int, width: Int, height: Int): RectangleInt {
		//val changed = (actualBounds.x != x || actualBounds.y != y || actualBounds.width != width || actualBounds.height != height)
		val resized = ((nativeBounds.width != width) || (nativeBounds.height != height))
		nativeBounds.setTo(x, y, width, height)
		//println("$actualBounds: $width,$height")
		actualBounds.setTo(x, y, width, height)
		lc.setBounds(handle, x, y, width, height)
		if (resized) {
			onResized(x, y, width, height)
			repaint()
		}
		//invalidateAncestors()
		return actualBounds
	}

	protected open fun onResized(x: Int, y: Int, width: Int, height: Int) {
	}

	open fun repaint() {
	}

	open fun recreate() {
		handle = lc.create(type)
		lc.setBounds(handle, nativeBounds.x, nativeBounds.y, nativeBounds.width, nativeBounds.height)
		for ((key, value) in properties) {
			lc.setProperty(handle, key, value)
		}
		lc.setParent(handle, parent?.handle)
	}

	open var parent: Container? = null
		set(newParent) {
			if (field != newParent) {
				val old = field
				if (newParent != null) {
					newParent.children -= this
				}
				field = newParent
				newParent?.children?.add(this)
				lc.setParent(handle, newParent?.handle)
				//invalidate()
				newParent?.invalidate()
				ancestorChanged(old, newParent)
			}
		}

	val root: Container? get() = parent?.root ?: (this as? Container?)
	val parentFrame: Frame? get() = (this as? Frame?) ?: parent?.parentFrame

	open fun ancestorChanged(old: Container?, newParent: Container?) {
	}

	fun invalidate() {
		//println("------invalidate")
		invalidateAncestors()
		invalidateDescendants()
	}

	open fun invalidateDescendants() {
		//println("------invalidateDescendants")
		valid = false
	}

	fun invalidateAncestors() {
		//println("------invalidateAncestors")
		if (!valid) return
		valid = false
		parent?.invalidateAncestors()
	}

	open fun setBoundsAndRelayout(x: Int, y: Int, width: Int, height: Int): RectangleInt {
		if (valid) return actualBounds
		valid = true
		return setBoundsInternal(x, y, width, height)
	}

	fun setBoundsAndRelayout(rect: RectangleInt) = setBoundsAndRelayout(rect.x, rect.y, rect.width, rect.height)

	//fun onClick(handler: (LightClickEvent) -> Unit) {
	//	lc.setEventHandler<LightClickEvent>(handle, handler)
	//}

	var mouseX = 0
	var mouseY = 0

	var visible by lightProperty(LightProperty.VISIBLE)

	override fun toString(): String = "Component($type)"

	fun focus() {
		lc.callAction(handle, LightAction.FOCUS, null)
	}
}

open class Container(app: Application, var layout: Layout, type: LightType = LightType.CONTAINER) :
	Component(app, type) {
	val children = arrayListOf<Component>()

	override fun recreate() {
		super.recreate()
		for (child in children) child.recreate()
	}

	override fun invalidateDescendants() {
		super.invalidateDescendants()
		for (child in children) child.invalidateDescendants()
	}

	override fun setBoundsAndRelayout(x: Int, y: Int, width: Int, height: Int): RectangleInt {
		//println("relayout:$valid")
		if (valid) return actualBounds
		//println("$this: relayout")
		valid = true
		return setBoundsInternal(layout.applyLayout(this, children, x, y, width, height, out = actualBounds))
	}

	fun <T : Component> add(other: T): T {
		other.parent = this
		return other
	}

	override fun ancestorChanged(old: Container?, newParent: Container?) {
		for (child in children) child.ancestorChanged(old, newParent)
	}

	override fun toString(): String = "Container($type)"
}

open class ScrollPane(app: Application, layout: Layout) : Container(app, layout, LightType.SCROLL_PANE) {
	override fun toString(): String = "ScrollPane"
}

class Frame(app: Application, title: String) : Container(app, LayeredLayout(app), LightType.FRAME) {
	var title by lightProperty(LightProperty.TEXT)
	var icon by lightProperty(LightProperty.ICON)
	var bgcolor by lightProperty(LightProperty.BGCOLOR)

	init {
		this.title = title
	}

	suspend fun dialogOpenFile(filter: String = ""): VfsFile {
		if (!lc.insideEventHandler) throw IllegalStateException("Can't open file dialog outside an event")
		return lc.dialogOpenFile(handle, filter)
	}

	suspend fun prompt(message: String, initialValue: String = ""): String =
		lc.dialogPrompt(handle, message, initialValue)

	suspend fun alert(message: String): Unit = lc.dialogAlert(handle, message)
	fun openURL(url: String): Unit = lc.openURL(url)

	fun onDropFiles(
		enter: () -> Boolean,
		exit: () -> Unit,
		drop: (List<VfsFile>) -> Unit
	): Closeable {
		return lc.addHandler(handle, object : LightDropHandler() {
			override fun enter(info: EnterInfo): Boolean = enter()
			override fun exit() = exit()
			override fun files(info: FileInfo) = drop(info.files)
		})
	}

	override fun toString(): String = "Frame"
}

class AgCanvas(app: Application) : Component(app, LightType.AGCANVAS), AGContainer {
	override val ag = componentInfo.ag!!
	override val agInput: AGInput = AGInput()

	private fun updateMouse(e: LightMouseHandler.Info) {
		agInput.mouseEvent.apply {
			x = e.x
			y = e.y
		}
	}

	private fun updateGamepad(e: LightGamepadHandler.Info) {
		agInput.gamepadEvent.gamepad.copyFrom(e.gamepad)
	}

	private fun updateKey(e: LightKeyHandler.Info) {
		agInput.keyEvent.keyCode = e.keyCode
	}

	private fun updateTouch(e: LightTouchHandler.Info) {
		agInput.touchEvent.id = e.id
		agInput.touchEvent.x = e.x
		agInput.touchEvent.y = e.y
	}

	init {
		onMouseUp { updateMouse(it); agInput.onMouseUp(agInput.mouseEvent) }
		onMouseDown { updateMouse(it); agInput.onMouseDown(agInput.mouseEvent) }
		onMouseOver { updateMouse(it); agInput.onMouseOver(agInput.mouseEvent) }
		onMouseDrag { updateMouse(it); agInput.onMouseDrag(agInput.mouseEvent) }
		onMouseClick { updateMouse(it); agInput.onMouseClick(agInput.mouseEvent) }

		onKeyDown { updateKey(it); agInput.onKeyDown(agInput.keyEvent) }
		onKeyUp { updateKey(it); agInput.onKeyUp(agInput.keyEvent) }
		onKeyTyped { updateKey(it); agInput.onKeyTyped(agInput.keyEvent) }

		onTouchStart { updateTouch(it); agInput.onTouchStart(agInput.touchEvent) }
		onTouchEnd { updateTouch(it); agInput.onTouchEnd(agInput.touchEvent) }
		onTouchMove { updateTouch(it); agInput.onTouchMove(agInput.touchEvent) }

		onGamepadUpdate { updateGamepad(it); agInput.onGamepadUpdate(agInput.gamepadEvent) }
		onGamepadConnection { updateGamepad(it); agInput.onGamepadConnection(agInput.gamepadEvent) }
	}

	//var registeredKeyEvents = false

	//override fun ancestorChanged(old: Container?, newParent: Container?) {
	//	if (!registeredKeyEvents) {
	//		registeredKeyEvents = true
	//		println("Registered AgCanvas.keyEvents to $parentFrame")
	//		parentFrame?.onKeyDown?.invoke { updateKey(it); agInput.onKeyDown(agInput.keyEvent) }
	//		parentFrame?.onKeyUp?.invoke { updateKey(it); agInput.onKeyUp(agInput.keyEvent) }
	//		parentFrame?.onKeyTyped?.invoke { updateKey(it); agInput.onKeyTyped(agInput.keyEvent) }
	//	}
	//}

	override fun repaint() {
		ag.repaint()
	}

	override fun onResized(x: Int, y: Int, width: Int, height: Int) {
		super.onResized(x, y, width, height)
		ag.resized()
	}

	fun onRender(callback: (ag: AG) -> Unit) {
		ag.onRender { callback(it) }
	}

	override fun toString(): String = "AGCanvas"
}

class Button(app: Application, text: String) : Component(app, LightType.BUTTON) {
	var text by lightProperty(LightProperty.TEXT)

	init {
		this.text = text
	}

	override fun toString(): String = "Button"
}

class Label(app: Application, text: String) : Component(app, LightType.LABEL) {
	var text by lightProperty(LightProperty.TEXT)

	init {
		this.text = text
	}

	override fun toString(): String = "Label"
}

class TextField(app: Application, text: String) : Component(app, LightType.TEXT_FIELD) {
	var text by lightProperty(LightProperty.TEXT, getable = true)

	init {
		this.text = text
	}

	override fun toString(): String = "TextField"
}

class TextArea(app: Application, text: String) : Component(app, LightType.TEXT_AREA) {
	var text by lightProperty(LightProperty.TEXT, getable = true)

	init {
		this.text = text
	}

	override fun toString(): String = "TextArea"
}

class CheckBox(app: Application, text: String, initialChecked: Boolean) : Component(app, LightType.CHECK_BOX) {
	var text by lightProperty(LightProperty.TEXT)
	var checked by lightProperty(LightProperty.CHECKED, getable = true)

	init {
		this.text = text
		this.checked = initialChecked
	}

	override fun toString(): String = "CheckBox"
}

class Progress(app: Application, current: Int = 0, max: Int = 100) : Component(app, LightType.PROGRESS) {
	var current by lightProperty(LightProperty.PROGRESS_CURRENT)
	var max by lightProperty(LightProperty.PROGRESS_MAX)

	fun set(current: Int, max: Int) {
		this.current = current
		this.max = max
	}

	init {
		set(current, max)
	}

	override fun toString(): String = "Progress"
}

class Spacer(app: Application) : Component(app, LightType.CONTAINER) {
	override fun toString(): String = "Spacer"
}

class Image(app: Application) : Component(app, LightType.IMAGE) {
	var image by lightProperty(LightProperty.IMAGE) {
		if (it != null) {
			if (this.style.defaultSize.width != it.width.pt || this.style.defaultSize.height != it.height.pt) {
				this.style.defaultSize.setTo(it.width.pt, it.height.pt)
				invalidate()
			}
		}
	}

	var smooth by lightProperty(LightProperty.IMAGE_SMOOTH)

	fun refreshImage() {
		setProperty(LightProperty.IMAGE, image, reset = true)
	}

	override fun toString(): String = "Image"
}

//fun Application.createFrame(): Frame = Frame(this.light)

fun <T : Component> T.setSize(width: Length, height: Length) = this.apply { this.style.size.setTo(width, height) }

suspend fun Container.button(text: String) = add(Button(this.app, text))
suspend inline fun Container.button(text: String, noinline callback: suspend Button.() -> Unit): Button =
	add(Button(this.app, text).apply {
		callback.await(this@apply)
	})

suspend inline fun Container.progress(current: Int, max: Int) = add(Progress(this.app, current, max))

fun Container.agCanvas() = agCanvas { }

inline fun Container.agCanvas(callback: AgCanvas.() -> Unit) = add(AgCanvas(this.app).apply {
	val canvas = this
	callback(canvas)
})

suspend inline fun Container.image(bitmap: Bitmap, noinline callback: suspend Image.() -> Unit) =
	add(Image(this.app).apply { image = bitmap; callback.await(this) })

suspend inline fun Container.image(bitmap: Bitmap) = add(Image(this.app).apply {
	image = bitmap
	this.style.defaultSize.width = bitmap.width.pt
	this.style.defaultSize.height = bitmap.height.pt
})

suspend inline fun Container.spacer() = add(Spacer(this.app))

suspend inline fun Container.label(text: String, noinline callback: suspend Label.() -> Unit = {}) =
	add(Label(this.app, text).apply { callback.await(this) })

suspend inline fun Container.checkBox(
	text: String,
	checked: Boolean = false,
	noinline callback: suspend CheckBox.() -> Unit = {}
) = add(CheckBox(this.app, text, checked).apply { callback.await(this) })

suspend inline fun Container.textField(text: String = "", noinline callback: suspend TextField.() -> Unit = {}) =
	add(TextField(this.app, text).apply { callback.await(this) })

suspend inline fun Container.textArea(text: String = "", noinline callback: suspend TextArea.() -> Unit = {}) =
	add(TextArea(this.app, text).apply { callback.await(this) })

suspend inline fun Container.layers(noinline callback: suspend Container.() -> Unit): Container =
	add(Container(this.app, LayeredLayout(app)).apply { callback.await(this) })

suspend inline fun Container.layersKeepAspectRatio(
	anchor: Anchor = Anchor.MIDDLE_CENTER,
	scaleMode: ScaleMode = ScaleMode.SHOW_ALL,
	noinline callback: suspend Container.() -> Unit
): Container {
	return add(Container(this.app, LayeredKeepAspectLayout(app, anchor, scaleMode)).apply { callback.await(this) })
}

suspend inline fun Container.vertical(noinline callback: suspend Container.() -> Unit): Container =
	add(Container(this.app, VerticalLayout(app)).apply { callback.await(this) })

suspend inline fun Container.horizontal(noinline callback: suspend Container.() -> Unit): Container {
	return add(Container(this.app, HorizontalLayout(app)).apply {
		callback.await(this)
	})
}

suspend inline fun Container.inline(noinline callback: suspend Container.() -> Unit): Container {
	return add(Container(this.app, InlineLayout(app)).apply {
		callback.await(this)
	})
}

suspend inline fun Container.relative(noinline callback: suspend Container.() -> Unit): Container {
	return add(Container(this.app, RelativeLayout(app)).apply {
		callback.await(this)
	})
}

suspend inline fun Container.scrollPane(noinline callback: suspend ScrollPane.() -> Unit): ScrollPane {
	return add(ScrollPane(this.app, ScrollPaneLayout(app)).apply {
		callback.await(this)
	})
}


fun <T : Component> T.click(handler: suspend Component.() -> Unit) =
	this.apply { onMouseClick { handler.execAndForget(coroutineContext, this) } }

fun <T : Component> T.mouseOver(handler: suspend Component.() -> Unit) =
	this.apply { onMouseOver { handler.execAndForget(coroutineContext, this) } }

fun <T : Component> T.mouseDrag(handler: suspend Component.() -> Unit) =
	this.apply { onMouseDrag { handler.execAndForget(coroutineContext, this) } }

fun <T : Component> T.mouseEnter(handler: suspend Component.() -> Unit) =
	this.apply { onMouseEnter { handler.execAndForget(coroutineContext, this) } }

fun <T : Component> T.mouseExit(handler: suspend Component.() -> Unit) =
	this.apply { onMouseExit { handler.execAndForget(coroutineContext, this) } }