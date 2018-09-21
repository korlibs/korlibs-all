package com.soywiz.korag

import com.soywiz.kgl.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.error.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.browser.*
import org.khronos.webgl.WebGLRenderingContext as GL

object AGFactoryWebgl : AGFactory {
	override val supportsNativeFrame: Boolean = true
	override fun create(nativeControl: Any?): AG = AGWebgl()
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
		TODO()
	}
}

fun jsEmptyObject(): dynamic = js("({})")

fun jsObject(vararg pairs: Pair<String, Any?>): dynamic {
	val out = jsEmptyObject()
	for ((k, v) in pairs) if (v != null) out[k] = v
	//for ((k, v) in pairs) out[k] = v
	return out
}

class AGWebgl : AGOpengl(), AGContainer {
	companion object {
		val log = Logger("AGWebgl")

		//var UNPACK_PREMULTIPLY_ALPHA_WEBGL = document.createElement('canvas').getContext('webgl').UNPACK_PREMULTIPLY_ALPHA_WEBGL
		const val UNPACK_PREMULTIPLY_ALPHA_WEBGL = 37441
	}

	override val ag: AG = this

	val canvas = document.createElement("canvas") as HTMLCanvasElement
	val glOpts = jsObject(
		"premultipliedAlpha" to true,
		"alpha" to false,
		"stencil" to true
	)
	//val gl: GL = (canvas.getContext("webgl", glOpts) ?: canvas.getContext("experimental-webgl", glOpts)) as GL
	override val gl = KmlGlCached(KmlGlJsCanvas(canvas, glOpts))

	init {
		(window.asDynamic()).ag = this
		//(window.asDynamic()).gl = gl
	}

	override val nativeComponent: Any = canvas
	val tDevicePixelRatio get() = window.devicePixelRatio.toDouble()
	override var devicePixelRatio = 1.0; get() = when {
		tDevicePixelRatio <= 0.0 -> 1.0
		tDevicePixelRatio.isNaN() -> 1.0
		tDevicePixelRatio.isInfinite() -> 1.0
		else -> tDevicePixelRatio
	}
	val onReadyOnce = Once()

	init {
		canvas.addEventListener("webglcontextlost", { e ->
			//contextVersion++
			e.preventDefault()
		}, false)

		canvas.addEventListener("webglcontextrestored", { e ->
			contextVersion++
			//e.preventDefault()
		}, false)

		//fun handleOnResized() {
		//	ag.resized(canvas.width, canvas.height)
		//}
//
		//window.addEventListener("resize", { e ->
		//	handleOnResized()
		//	//e.preventDefault()
		//}, false)
//
		//handleOnResized()
	}

	override fun repaint() {
		onReadyOnce { ready() }
		onRender(this)
	}

	override fun dispose() {
		// https://www.khronos.org/webgl/wiki/HandlingContextLost
		// https://gist.github.com/mattdesl/9995467
	}

	override fun prepareUploadNativeTexture(bmp: NativeImage) {
		gl.pixelStorei(UNPACK_PREMULTIPLY_ALPHA_WEBGL, bmp.premult.toInt())
	}
}
