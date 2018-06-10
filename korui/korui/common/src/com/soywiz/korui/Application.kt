package com.soywiz.korui

import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.light.*
import com.soywiz.korui.ui.*
import kotlin.coroutines.experimental.*

suspend fun Application(light: LightComponents = defaultLight) = withCoroutineContext {
	Application(this@withCoroutineContext, light)
}

class Application(val coroutineContext: CoroutineContext, val light: LightComponents = defaultLight) {
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
	callback.await(frame)
	light.setBounds(frame.handle, 0, 0, frame.actualBounds.width, frame.actualBounds.height)
	light.addHandler(frame.handle, object : LightResizeHandler() {
		override fun resized(e: Info) {
			frame.setBoundsInternal(0, 0, e.width, e.height)
			frame.invalidate()
		}
	})
	frames += frame
	frame.visible = true
	frame.invalidate()
	return frame
}


suspend fun CanvasApplication(
	title: String,
	width: Int = 640,
	height: Int = 480,
	icon: Bitmap? = null,
	light: LightComponents = defaultLight,
	callback: suspend (AGContainer) -> Unit = {}
): Unit {
	//if (agFactory.supportsNativeFrame) {
	//	val win = agFactory.createFastWindow(title, width, height)
	//	callback(win)
	//} else {
	val application = Application(getCoroutineContext(), light)
	application.frame(title, width, height, icon) {
		callback(agCanvas().apply { focus() })
	}
	//}
	Unit
}

suspend fun CanvasApplicationEx(
	title: String,
	width: Int = 640,
	height: Int = 480,
	icon: Bitmap? = null,
	light: LightComponents = defaultLight,
	callback: suspend (AGContainer, Frame) -> Unit = { c, f -> }
): Unit {
	//if (agFactory.supportsNativeFrame) {
	//	val win = agFactory.createFastWindow(title, width, height)
	//	callback(win)
	//} else {
	val application = Application(getCoroutineContext(), light)
	application.frame(title, width, height, icon) {
		callback(agCanvas().apply { focus() }, this)
	}
	//}
	Unit
}