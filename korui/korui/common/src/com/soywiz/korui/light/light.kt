package com.soywiz.korui.light

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korui.event.*
import kotlin.reflect.*

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

	private val eds = LinkedHashMap<Any, EventDispatcher>()
	fun getEventListener(c: Any): EventDispatcher {
		return eds.getOrPut(c) {
			object : EventDispatcher.Mixin() {
				val registeredClasses = LinkedHashSet<KClass<out Event>>()

				override fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Closeable {
					if (clazz !in registeredClasses) {
						registeredClasses += clazz
						registerEventKind(c, clazz, this)
					}
					return super.addEventListener(clazz, handler)
				}
			}
		}
	}

	fun <T : Event> register(c: Any, clazz: KClass<T>, handler: (T) -> Unit): Closeable {
		return getEventListener(c).addEventListener(clazz, handler)
	}

	protected open fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
		return Closeable { }
	}

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
	open fun configuredFrame(handle: Any) {
	}
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

var LightComponents.LightComponentInfo.ag: AG? by extraProperty("ag") { null }
