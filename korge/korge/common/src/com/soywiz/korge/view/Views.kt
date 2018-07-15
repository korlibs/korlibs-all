package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korge.*
import com.soywiz.korge.audio.*
import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.stat.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.event.*
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.*
import kotlin.math.*
import kotlin.reflect.*

private val logger = Logger("Views")

@Singleton
class Views(
	override val coroutineContext: CoroutineContext,
	val ag: AG,
	val injector: AsyncInjector,
	val input: Input,
	val timeProvider: TimeProvider,
	val stats: Stats
) : Updatable, Extra by Extra.Mixin(), EventDispatcher by EventDispatcher.Mixin(), CoroutineContextHolder {

	var imageFormats = defaultImageFormats

	fun dumpStats() {
		stats.dump()
	}

	init {
		logger.trace { "Views[0]" }
	}

	init {
		logger.trace { "Views[1]" }
	}

	var lastId = 0

	init {
		logger.trace { "Views[2]" }
	}

	val renderContext = RenderContext(ag)

	init {
		logger.trace { "Views[3]" }
	}

	var clearEachFrame = true
	val views = this

	init {
		logger.trace { "Views[4]" }
	}

	init {
		injector.mapInstance(CoroutineContext::class, coroutineContext)
		injector.mapInstance(AG::class, ag)
		injector.mapInstance(Views::class, this)
		injector.mapInstance(SoundSystem::class, soundSystem)
	}

	init {
		logger.trace { "Views[5]" }
	}


	val propsTriggers = hashMapOf<String, (View, String, String) -> Unit>()

	fun registerPropertyTrigger(propName: String, gen: (View, String, String) -> Unit) {
		propsTriggers[propName] = gen
	}

	fun registerPropertyTriggerSuspend(propName: String, gen: suspend (View, String, String) -> Unit) {
		propsTriggers[propName] = { view, key, value ->
			launch(coroutineContext) {
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

	val virtualLeft get() = -actualVirtualLeft * views.stage.scaleX
	val virtualTop get() = -actualVirtualTop * views.stage.scaleY
	val virtualRight get() = virtualLeft + virtualWidth * views.stage.scaleX
	val virtualBottom get() = virtualTop + virtualHeight * views.stage.scaleY

	val actualVirtualRight get() = actualVirtualWidth
	val actualVirtualBottom get() = actualVirtualHeight

	val nativeMouseX: Double get() = input.mouse.x
	val nativeMouseY: Double get() = input.mouse.y

	//var actualVirtualWidth = ag.backWidth
	//var actualVirtualHeight = ag.backHeight

	//var scaleMode: ScaleMode = ScaleMode.COVER
	//var scaleMode: ScaleMode = ScaleMode.NO_SCALE
	var scaleMode: ScaleMode = ScaleMode.SHOW_ALL
	var scaleAnchor = Anchor.MIDDLE_CENTER
	var clipBorders = true

	override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
		try {
			this.stage.dispatch(clazz, event)
		} catch (e: PreventDefaultException) {
			//println("PreventDefaultException.Reason: ${e.reason}")
		}
	}

	private val resizedEvent = ResizedEvent(0, 0)

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

	var lastTime = timeProvider.currentTimeMillis()

	fun frameUpdateAndRender(clear: Boolean, clearColor: Int, fixedSizeStep: Int? = null) {
		views.stats.startFrame()
		Korge.logger.trace { "ag.onRender" }
		//println("Render")
		val currentTime = timeProvider.currentTimeMillis()
		//println("currentTime: $currentTime")
		val delta = (currentTime - lastTime).toInt()
		val adelta = min(delta, views.clampElapsedTimeTo)
		//println("delta: $delta")
		//println("Render($lastTime -> $currentTime): $delta")
		lastTime = currentTime
		if (fixedSizeStep != null) {
			update(fixedSizeStep)
		} else {
			update(adelta)
		}
		render(clearColor, clear)
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

		stage.dispatch(resizedEvent.apply {
			this.width = actualSize.width
			this.height = actualSize.height
		})

		stage.invalidate()
	}

	var targetFps: Double = -1.0

	fun dispose() {
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

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (views.clipBorders) {
			ctx.ctx2d.scissor(
				AG.Scissor(
					x.toInt(), y.toInt(), (views.virtualWidth * scaleX).toInt(),
					(views.virtualHeight * scaleY).toInt()
				)
			) {
				super.render(ctx, m)
			}
		} else {
			super.render(ctx, m)
		}
	}
}

class ViewsLog(
	val coroutineContext: CoroutineDispatcher,
	val injector: AsyncInjector = AsyncInjector(),
	val ag: LogAG = LogAG(),
	val input: Input = Input(),
	val timeProvider: TimeProvider = TimeProvider(),
	val stats: Stats = Stats()
) {
	val views = Views(coroutineContext, ag, injector, input, timeProvider, stats)
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

fun Bitmap.texture(views: Views, mipmaps: Boolean = false) = views.texture(this, mipmaps)

fun Views.texture(width: Int, height: Int, mipmaps: Boolean = false): Texture {
	return texture(Bitmap32(width, height), mipmaps)
}

suspend fun Views.texture(bmp: ByteArray, mipmaps: Boolean = false): Texture {
	return texture(nativeImageFormatProvider.decode(bmp), mipmaps)
}

interface ViewsContainer {
	val views: Views
}

val ViewsContainer.ag: AG get() = views.ag

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
