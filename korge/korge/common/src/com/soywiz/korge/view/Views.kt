package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korge.audio.*
import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.event.*
import com.soywiz.korge.input.*
import com.soywiz.korge.plugin.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

@Singleton
class Views(
	val eventLoop: EventLoop,
	val ag: AG,
	val injector: AsyncInjector,
	val input: Input,
	val plugins: KorgePlugins
) : AsyncDependency, Updatable, Extra by Extra.Mixin(), EventDispatcher by EventDispatcher.Mixin(),
	CoroutineContextHolder {
	override val coroutineContext = eventLoop.coroutineContext
	var lastId = 0
	val renderContext = RenderContext(ag)
	var clearEachFrame = true
	val views = this

	init {
		injector.mapInstance(EventLoop::class, eventLoop)
		injector.mapInstance(AG::class, ag)
		injector.mapInstance(Views::class, this)
		injector.mapInstance(SoundSystem::class, soundSystem)
	}

	val propsTriggers = hashMapOf<String, (View, String, String) -> Unit>()

	fun registerPropertyTrigger(propName: String, gen: (View, String, String) -> Unit) {
		propsTriggers[propName] = gen
	}

	fun registerPropertyTriggerSuspend(propName: String, gen: suspend (View, String, String) -> Unit) {
		propsTriggers[propName] = { view, key, value ->
			eventLoop.go {
				gen(view, key, value)
			}
		}
	}

	var clampElapsedTimeTo = 100

	val nativeWidth get() = ag.backWidth
	val nativeHeight get() = ag.backHeight

	var virtualWidth = 640; internal set
	var virtualHeight = 480; internal set

	fun setVirtualSize(width: Int, height: Int) {
		this.virtualWidth = width
		this.virtualHeight = height
		resized()
	}

	var actualVirtualLeft = 0; private set
	var actualVirtualTop = 0; private set

	var actualVirtualWidth = 640; private set
	var actualVirtualHeight = 480; private set

	val nativeMouseX: Double get() = input.mouse.x
	val nativeMouseY: Double get() = input.mouse.y

	//var actualVirtualWidth = ag.backWidth
	//var actualVirtualHeight = ag.backHeight

	//var scaleMode: ScaleMode = ScaleMode.COVER
	//var scaleMode: ScaleMode = ScaleMode.NO_SCALE
	var scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	var scaleAnchor = Anchor.MIDDLE_CENTER

	suspend override fun init() {
		for (plugin in plugins.plugins) plugin.register(this)
	}

	override fun <T : Any> dispatch(event: T, clazz: KClass<out T>) {
		try {
			this.stage.dispatch(event, clazz)
		} catch (e: PreventDefaultException) {
			//println("PreventDefaultException.Reason: ${e.reason}")
		}
	}

	private val resizedEvent = StageResizedEvent(0, 0)

	fun container() = Container(this)
	fun fixedSizeContainer(width: Double = 100.0, height: Double = 100.0) = FixedSizeContainer(this, width, height)
	inline fun solidRect(width: Number, height: Number, color: Int): SolidRect =
		SolidRect(this, width.toDouble(), height.toDouble(), color)

	val dummyView by lazy { View(this) }
	val transparentTexture by lazy { texture(Bitmap32(0, 0)) }
	val whiteTexture by lazy { texture(Bitmap32(1, 1, intArrayOf(Colors.WHITE))) }
	val transformedDummyTexture by lazy { TransformedTexture(transparentTexture) }
	val dummyFont by lazy { BitmapFont(ag, 16, IntMap(), IntMap()) }
	val defaultFont by lazy {
		com.soywiz.korim.font.BitmapFontGenerator.generate("Arial", 16, BitmapFontGenerator.LATIN_ALL)
			.convert(ag, mipmaps = true)
	}
	val fontRepository = FontRepository(this)

	val stage = Stage(this)
	var debugViews = false
	val debugHandlers = arrayListOf<Views.() -> Unit>()

	fun render(clearColor: Int = Colors.BLACK, clear: Boolean = true) {
		if (clear) ag.clear(clearColor, stencil = 0, clearColor = true, clearStencil = true)
		stage.render(renderContext)

		if (debugViews) {
			for (debugHandler in debugHandlers) {
				this.debugHandler()
			}
		}

		renderContext.flush()
		renderContext.finish()
	}

	fun dump(emit: (String) -> Unit = ::println) = dumpView(stage, emit)

	fun dumpView(view: View, emit: (String) -> Unit = ::println, indent: String = "") {
		emit("$indent$view")
		if (view is Container) {
			for (child in view.children) {
				dumpView(child, emit, "$indent ")
			}
		}
	}

	override fun update(dtMs: Int) {
		//println(this)
		//println("Update: $dtMs")
		input.startFrame(dtMs)
		stage.update(dtMs)
		input.endFrame(dtMs)
	}

	private val virtualSize = SizeInt()
	private val actualSize = SizeInt()
	private val targetSize = SizeInt()

	fun mouseUpdated() {
		//println("localMouse: (${stage.localMouseX}, ${stage.localMouseY}), inputMouse: (${input.mouse.x}, ${input.mouse.y})")
	}

	fun resized(width: Int, height: Int) {
		val actualWidth = width
		val actualHeight = height
		//println("RESIZED: $width, $height")
		actualSize.setTo(actualWidth, actualHeight)
		resized()
	}

	fun resized() {
		//println("$e : ${views.ag.backWidth}x${views.ag.backHeight}")
		val virtualWidth = virtualWidth
		val virtualHeight = virtualHeight
		val anchor = scaleAnchor

		virtualSize.setTo(virtualWidth, virtualHeight)

		scaleMode(virtualSize, actualSize, targetSize)

		val ratioX = targetSize.width.toDouble() / virtualWidth.toDouble()
		val ratioY = targetSize.height.toDouble() / virtualHeight.toDouble()

		actualVirtualWidth = (actualSize.width / ratioX).toInt()
		actualVirtualHeight = (actualSize.height / ratioY).toInt()

		stage.scaleX = ratioX
		stage.scaleY = ratioY

		stage.x = (((actualVirtualWidth - virtualWidth) * anchor.sx) * ratioX).toInt().toDouble()
		stage.y = (((actualVirtualHeight - virtualHeight) * anchor.sy) * ratioY).toInt().toDouble()

		actualVirtualLeft = -(stage.x / ratioX).toInt()
		actualVirtualTop = -(stage.y / ratioY).toInt()

		stage.dispatch(resizedEvent.setSize(actualSize.width, actualSize.height))
	}

	var targetFps: Double = -1.0

	fun animationFrameLoop(callback: () -> Unit): Closeable {
		println("Views.animationFrameLoop.eventLoop: $eventLoop")
		//val process = async {
		//	var accumulatedMs = 0.0
		//	while (true) {
		//		callback()
		//		if (targetFps <= 0) {
		//			eventLoop.sleepNextFrame()
		//		} else {
		//			val targetMs = (1000.0 / targetFps) + accumulatedMs
		//			val targetMsInt = targetMs.toInt()
		//			accumulatedMs = targetMs - targetMsInt
		//			eventLoop.sleep(targetMsInt)
		//		}
		//	}
		//}
		//return Closeable { process.cancel() }
		return eventLoop.animationFrameLoop(callback)
	}

	fun dispose() {
		eventLoop.close()
		soundSystem.close()
	}
}

class Stage(views: Views) : Container(views) {
	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(views.actualVirtualLeft, views.actualVirtualTop, views.actualVirtualWidth, views.actualVirtualHeight)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		return super.hitTestInternal(x, y) ?: this
	}

	override fun hitTestBoundingInternal(x: Double, y: Double): View? {
		return super.hitTestBoundingInternal(x, y) ?: this
	}
}

class ViewsLog(
	val eventLoop: EventLoop,
	val injector: AsyncInjector = AsyncInjector(),
	val ag: LogAG = LogAG(),
	val input: Input = Input(),
	val plugins: KorgePlugins = defaultKorgePlugins
) : AsyncDependency {
	val views = Views(eventLoop, ag, injector, input, plugins)

	suspend override fun init() {
		views.init()
	}
}

/*
object ViewFactory {
    inline fun Container.container(): Container {
        val container = views.container()
        this += container
        return container
    }
}

inline fun viewFactory(callback: ViewFactory.() -> Unit) {
    ViewFactory.callback()
}
*/

inline fun Container.container(): Container = container { }

inline fun Container.container(callback: Container.() -> Unit): Container {
	val child = views.container()
	this += child
	callback(child)
	return child
}

fun Views.texture(bmp: Bitmap, mipmaps: Boolean = false): Texture {
	return Texture(Texture.Base(ag.createTexture(bmp, mipmaps), bmp.width, bmp.height))
}

suspend fun Views.texture(bmp: ByteArray, mipmaps: Boolean = false): Texture {
	return texture(nativeImageFormatProvider.decode(bmp), mipmaps)
}

interface ViewsContainer {
	val views: Views
}

data class KorgeFileLoaderTester<T>(
	val name: String,
	val tester: suspend (s: FastByteArrayInputStream, injector: AsyncInjector) -> KorgeFileLoader<T>?
) {
	suspend operator fun invoke(s: FastByteArrayInputStream, injector: AsyncInjector) = tester(s, injector)
	override fun toString(): String = "KorgeFileTester(\"$name\")"
}

data class KorgeFileLoader<T>(val name: String, val loader: suspend VfsFile.(FastByteArrayInputStream, Views) -> T) {
	override fun toString(): String = "KorgeFileLoader(\"$name\")"
}

//suspend val AsyncInjector.views: Views get() = this.get<Views>()

data class StageResizedEvent(var width: Int, var height: Int) : Event {
	fun setSize(width: Int, height: Int) = this.apply {
		this.width = width
		this.height = height
	}
}

interface MouseEvent : Event
class MouseClickEvent : MouseEvent
class MouseUpEvent : MouseEvent
class MouseDownEvent : MouseEvent
class MouseOverEvent : MouseEvent
class MouseDragEvent : MouseEvent
interface KeyEvent : Event {
	var keyCode: Int
}

class KeyDownEvent(override var keyCode: Int = 0) : KeyEvent
class KeyUpEvent(override var keyCode: Int = 0) : KeyEvent
class KeyTypedEvent(override var keyCode: Int = 0) : KeyEvent

data class TouchEvent(var touch: Input.Touch, var start: Boolean, var end: Boolean) : Event {
	fun set(touch: Input.Touch, start: Boolean, end: Boolean) = this.apply {
		this.touch = touch
		this.start = start
		this.end = end
	}
}

class GamepadUpdatedEvent(
	val gamepad: GamepadInfo = GamepadInfo()
) : Event

class GamepadConnectionEvent(
	val gamepad: GamepadInfo = GamepadInfo()
) : Event {
	val index get() = gamepad.index
	val connected get() = gamepad.connected
}
