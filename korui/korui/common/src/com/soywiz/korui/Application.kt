package com.soywiz.korui

import com.soywiz.klogger.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.*
import com.soywiz.korui.event.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.light.*
import com.soywiz.korui.ui.*
import kotlin.coroutines.experimental.*

suspend fun Application() = Application(defaultLight(coroutineContext))
suspend fun Application(light: LightComponents) = Application(coroutineContext, light)

class Application(val coroutineContext: CoroutineContext, val light: LightComponents) {
	val frames = arrayListOf<Frame>()
	val lengthContext = Length.Context().apply {
		pixelsPerInch = light.getDpi()
	}

	init {
		coroutineContext.eventLoop.animationFrameLoop {
			var n = 0
			while (n < frames.size) {
				val frame = frames[n++]
				if (frame.valid) continue
				frame.setBoundsAndRelayout(frame.actualBounds)
				light.repaint(frame.handle)
			}
		}
	}
}

private val koruiApplicationLog = Logger("korui-application")

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
	light.setBounds(frame.handle, 0, 0, frame.actualBounds.width, frame.actualBounds.height)
	koruiApplicationLog.info { "Application.frame: ${frame.actualBounds}" }
	frame.addEventListener<ResizedEvent> { e ->
		koruiApplicationLog.info { "Application.frame.ResizedEvent: ${e.width},${e.height}" }
		frame.setBoundsInternal(0, 0, e.width, e.height)
		frame.invalidate()
	}
	callback.await(frame)
	frames += frame
	frame.visible = true
	frame.invalidate()
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
	val application = Application(getCoroutineContext(), llight)
	application.frame(title, width, height, icon) {
		val canvas = agCanvas().apply { focus() }
		llight.configuredFrame(handle)
		callback(canvas, this)
	}
	Unit
}