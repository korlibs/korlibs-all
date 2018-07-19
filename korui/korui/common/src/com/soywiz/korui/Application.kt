package com.soywiz.korui

import com.soywiz.klogger.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korui.event.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.light.*
import com.soywiz.korui.ui.*
import kotlin.coroutines.experimental.*

class Application(val coroutineContext: CoroutineContext, val light: LightComponents) : Closeable {
	companion object {
		suspend operator fun invoke() = Application(defaultLight(coroutineContext))
		suspend operator fun invoke(light: LightComponents) = Application(coroutineContext, light)

		suspend operator fun invoke(light: LightComponents, callback: suspend Application.() -> Unit) {
			val app = Application(coroutineContext, light)
			try {
				callback(app)
			} finally {
				app.loop.close()
			}
		}
	}

	val frames = arrayListOf<Frame>()
	val lengthContext = Length.Context().apply {
		pixelsPerInch = light.getDpi()
	}
	val devicePixelRatio: Double get() = light.getDevicePixelRatio()

	val loop = coroutineContext.animationFrameLoop {
		var n = 0
		while (n < frames.size) {
			val frame = frames[n++]
			if (frame.valid) continue
			frame.setBoundsAndRelayout(frame.actualBounds)
			light.repaint(frame.handle)
		}
	}

	override fun close() {
		loop.close()
	}
}

private val koruiApplicationLog = Logger("korui-application")

fun Application(callback: suspend Application.() -> Unit) =
	Korui { Application(defaultLightFactory.create(coroutineContext)) { callback() } }

suspend fun Application.frame(
	title: String,
	width: Int = 640,
	height: Int = 480,
	icon: Bitmap? = null,
	callback: suspend Frame.() -> Unit = {}
): Frame {
	val frame = Frame(this, title).apply {
		setBoundsInternal(0, 0, width, height)
	}
	frame.icon = icon
	//light.setBounds(frame.handle, 0, 0, frame.actualBounds.width, frame.actualBounds.height)
	koruiApplicationLog.info { "Application.frame: ${frame.actualBounds}" }
	var resizing = false
	frame.addEventListener<ResizedEvent> { e ->
		if (!resizing) {
			resizing = true
			try {
				koruiApplicationLog.info { "Application.frame.ResizedEvent: ${e.width},${e.height}" }
				//frame.setBoundsInternal(0, 0, e.width, e.height)
				//frame.invalidate()
				frame.setBoundsAndRelayout(0, 0, e.width, e.height)
				frame.invalidate()
				light.repaint(frame.handle)
			} finally {
				resizing = false
			}
		}
	}
	callback.await(frame)
	frames += frame
	frame.setBoundsAndRelayout(0, 0, frame.actualBounds.width, frame.actualBounds.height)
	frame.invalidate()
	frame.visible = true
	light.configuredFrame(frame.handle)
	return frame
}

//suspend fun CanvasApplication(
//	title: String,
//	width: Int = 640,
//	height: Int = 480,
//	icon: Bitmap? = null,
//	light: LightComponents = defaultLight,
//	callback: suspend (AGContainer) -> Unit = {}
//) = CanvasApplicationEx(title, width, height, icon, light) { canvas, _ -> callback(canvas) }

suspend fun CanvasApplicationEx(
	title: String,
	width: Int = 640,
	height: Int = 480,
	icon: Bitmap? = null,
	light: LightComponents? = null,
	quality: LightQuality = LightQuality.PERFORMANCE,
	callback: suspend (AgCanvas, Frame) -> Unit = { c, f -> }
): Unit {
	val llight = light ?: defaultLight(coroutineContext)
	llight.quality = quality
	val application = Application(coroutineContext, llight)
	lateinit var canvas: AgCanvas
	val frame = application.frame(title, width, height, icon) {
		canvas = agCanvas().apply { focus() }
	}
	callback(canvas, frame)
	Unit
}

