package com.soywiz.korag

import com.jogamp.newt.opengl.*
import com.jogamp.opengl.*
import com.jogamp.opengl.awt.*
import com.soywiz.kgl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.util.*
import java.nio.*


object AGFactoryAwt : AGFactory {
	override val supportsNativeFrame: Boolean = true
	override fun create(nativeControl: Any?): AG = AGAwt()
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
		val glp = GLProfile.getDefault()
		val caps = GLCapabilities(glp)
		val window = GLWindow.create(caps)
		window.title = title
		window.setSize(width, height)
		window.isVisible = true

		window.addGLEventListener(object : GLEventListener {
			override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) = Unit
			override fun display(drawable: GLAutoDrawable) = Unit
			override fun init(drawable: GLAutoDrawable) = Unit
			override fun dispose(drawable: GLAutoDrawable) = Unit
		})

		return object : AGWindow() {
			override fun repaint() = Unit
			override val ag: AG = AGAwtNative(window)
		}
	}
}

abstract class AGAwtBase : AGOpengl() {
	var glprofile = GLProfile.getDefault()
	var glcapabilities = GLCapabilities(glprofile).apply {
		stencilBits = 8
		depthBits = 24
	}
	var initialized = false
	lateinit var ad: GLAutoDrawable
	override lateinit var gl: JvmKmlGl
	lateinit var glThread: Thread

	override var devicePixelRatio: Double = 1.0

	protected fun setAutoDrawable(d: GLAutoDrawable) {
		glThread = Thread.currentThread()
		ad = d
		gl = JvmKmlGl(d.gl as GL2)
		initialized = true
	}

	val awtBase = this

	//val queue = LinkedList<(gl: GL) -> Unit>()
}

class AGAwt : AGAwtBase(), AGContainer {
	val glcanvas = GLCanvas(glcapabilities)
	override val nativeComponent = glcanvas

	override val ag: AG = this

	override fun offscreenRendering(callback: () -> Unit) {
		if (!glcanvas.context.isCurrent) {
			glcanvas.context.makeCurrent()
			try {
				callback()
			} finally {
				glcanvas.context.release()
			}
		} else {
			callback()
		}
	}

	override fun dispose() {
		glcanvas.disposeGLEventListener(glEventListener, true)
	}

	override fun repaint() {
		glcanvas.repaint()
		//if (initialized) {
		//	onRender(this)
		//}
	}

	override fun resized() {
		onResized(Unit)
	}

	private val tempFloat4 = FloatArray(4)

	val glEventListener = object : GLEventListener {
		override fun reshape(d: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
			setAutoDrawable(d)

			val (scaleX, scaleY) = glcanvas.getCurrentSurfaceScale(tempFloat4)
			devicePixelRatio = (scaleX + scaleY) / 2.0
			setViewport(0, 0, width, height)

			resized()
		}

		var onReadyOnce = Once()

		override fun display(d: GLAutoDrawable) {
			setAutoDrawable(d)

			//while (true) {
			//	val callback = synchronized(queue) { if (queue.isNotEmpty()) queue.remove() else null } ?: break
			//	callback(gl)
			//}

			onReadyOnce {
				ready()
			}
			onRender(awtBase)
			checkErrors { gl.flush() }

			//gl.glClearColor(1f, 1f, 0f, 1f)
			//gl.glClear(GL.GL_COLOR_BUFFER_BIT)
			//d.swapBuffers()
		}

		override fun init(d: GLAutoDrawable) {
			contextVersion++
			setAutoDrawable(d)
			//println("c")
		}

		override fun dispose(d: GLAutoDrawable) {
			setAutoDrawable(d)
			//println("d")
		}
	}

	init {
		//((glcanvas as JoglNewtAwtCanvas).getNativeWindow() as JAWTWindow).setSurfaceScale(new float[] {2, 2});
		//glcanvas.nativeSurface.
		//println(glcanvas.nativeSurface.convertToPixelUnits(intArrayOf(1000)).toList())

		glcanvas.addGLEventListener(glEventListener)
	}

	//override fun readColor(bitmap: Bitmap32): Unit {
	//	checkErrors {
	//		gl.readPixels(
	//			0, 0, bitmap.width, bitmap.height,
	//			GL.GL_RGBA, GL.GL_UNSIGNED_BYTE,
	//			IntBuffer.wrap(bitmap.data)
	//		)
	//	}
	//}

	//override fun readDepth(width: Int, height: Int, out: FloatArray): Unit {
	//	val GL_DEPTH_COMPONENT = 0x1902
	//	checkErrors { gl.readPixels(0, 0, width, height, GL_DEPTH_COMPONENT, GL.GL_FLOAT, FloatBuffer.wrap(out)) }
	//}
}

class AGAwtNative(override val nativeComponent: Any) : AGAwtBase() {

}
