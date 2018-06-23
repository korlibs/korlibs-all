package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.coroutine.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import com.soywiz.korma.*
import org.w3c.dom.*
import org.w3c.dom.url.*
import org.w3c.files.*
import kotlin.browser.*
import kotlin.math.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = HtmlNativeImageFormatProvider

class CanvasNativeImage(val canvas: HTMLCanvasElement) :
	NativeImage(canvas.width.toInt(), canvas.height.toInt(), canvas, false) {
	override val name: String = "CanvasNativeImage"

	override fun toNonNativeBmp(): Bitmap {
		val data = IntArray(width * height)
		HtmlImage.renderHtmlCanvasIntoBitmap(canvas, data)
		return Bitmap32(width, height, data)
	}

	override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(CanvasContext2dRenderer(canvas))
}

object HtmlNativeImageFormatProvider : NativeImageFormatProvider() {
	override suspend fun decode(data: ByteArray): NativeImage {
		return CanvasNativeImage(BrowserImage.decodeToCanvas(data))
	}

	override suspend fun decode(vfs: Vfs, path: String): NativeImage {
		//println("HtmlImageSpecialReader.readSpecial: $vfs, $path")
		val canvas = when (vfs) {
			is LocalVfs -> {
				//println("LOCAL: HtmlImageSpecialReader: $vfs, $path")
				BrowserImage.loadImage(path)
			}
			is UrlVfs -> {
				val jsUrl = vfs.getFullUrl(path)
				//println("URL: HtmlImageSpecialReader: $vfs, $path : $jsUrl")
				BrowserImage.loadImage(jsUrl)
			}
			else -> {
				//println("OTHER: HtmlImageSpecialReader: $vfs, $path")
				BrowserImage.decodeToCanvas(vfs[path].readAll())
			}
		}
		return CanvasNativeImage(canvas)
	}

	override fun create(width: Int, height: Int): NativeImage {
		return CanvasNativeImage(HtmlCanvas.createCanvas(width, height))
	}

	override fun copy(bmp: Bitmap): NativeImage {
		return CanvasNativeImage(HtmlImage.bitmapToHtmlCanvas(bmp.toBMP32()))
	}

	override suspend fun display(bitmap: Bitmap, kind: Int) {
		if (kind == 1) {
			val img = document.createElement("img")
			img.setAttribute("src", "data:image/png;base64," + PNG.encode(bitmap).toBase64())
			document.body?.appendChild(img)
		} else {
			val img = bitmap.toHtmlNative()
			document.body?.appendChild(img.canvas)
		}
	}

	override fun mipmap(bmp: Bitmap, levels: Int): NativeImage {
		var out = bmp.ensureNative()
		for (n in 0 until levels) out = mipmap(out)
		return out
	}

	override fun mipmap(bmp: Bitmap): NativeImage {
		val out = NativeImage(ceil(bmp.width * 0.5).toInt(), ceil(bmp.height * 0.5).toInt())
		out.getContext2d(antialiasing = true).renderer.drawImage(bmp, 0, 0, out.width, out.height)
		return out
	}
}

@Suppress("unused")
object BrowserImage {
	suspend fun decodeToCanvas(bytes: ByteArray): HTMLCanvasElement {
		val blob = Blob(arrayOf(bytes), BlobPropertyBag(type = "image/png"))
		val blobURL = URL.createObjectURL(blob)
		try {
			return loadImage(blobURL)
		} finally {
			URL.revokeObjectURL(blobURL)
		}
	}

	suspend fun loadImage(jsUrl: String): HTMLCanvasElement = korioSuspendCoroutine { c ->
		// Doesn't work with Kotlin.JS
		//val img = document.createElement("img") as HTMLImageElement
		//println("[1]")
		if (OS.isNodejs) {
			js("(require('canvas'))").loadImage(jsUrl).then({ v ->
				c.resume(v)
			}, { v ->
				c.resumeWithException(v)
			})
			Unit
		} else {
			//println("[a]")
			val img = document.createElement("img").unsafeCast<HTMLImageElement>()
			//println("[b]")
			img.onload = {
				//println("[onload.a]")
				val canvas: HTMLCanvasElement = HtmlCanvas.createCanvas(img.width, img.height)
				//println("[onload.b]")
				val ctx: CanvasRenderingContext2D = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
				//println("[onload.c]")
				ctx.drawImage(img, 0.0, 0.0)
				//println("[onload.d]")
				c.resume(canvas)
				//println("[onload.e]")
			}
			//println("[c]")
			img.onerror = { _, _, _, _, _ ->
				//println("[onerror.a]")
				c.resumeWithException(RuntimeException("error loading image $jsUrl"))
			}
			//println("[d]")
			//println("image: $jsUrl")
			img.src = jsUrl
			//document.body?.appendChild(img)
			//println("[e]")
			Unit
		}
	}
}

class CanvasContext2dRenderer(private val canvas: HTMLCanvasElement) : Context2d.Renderer() {
	override val width: Int get() = canvas.width.toInt()
	override val height: Int get() = canvas.height.toInt()

	val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()

	fun Context2d.Paint.toJsStr(): Any? {
		return when (this) {
			is Context2d.None -> "none"
			is Context2d.Color -> Colors.toHtmlStringSimple(this.color)
			is Context2d.Gradient -> {
				when (kind) {
					Context2d.Gradient.Kind.LINEAR -> {
						val grad = ctx.createLinearGradient(this.x0, this.y0, this.x1, this.y1)
						for (n in 0 until this.stops.size) {
							val stop = this.stops[n]
							val color = this.colors[n]
							grad.addColorStop(stop, Colors.toHtmlStringSimple(color))
						}
						grad
					}
					Context2d.Gradient.Kind.RADIAL -> {
						val grad = ctx.createRadialGradient(this.x0, this.y0, this.r0, this.x1, this.y1, this.r1)
						for (n in 0 until this.stops.size) {
							val stop = this.stops[n]
							val color = this.colors[n]
							grad.addColorStop(stop, Colors.toHtmlStringSimple(color))
						}
						grad
					}
				}
			}
			is Context2d.BitmapPaint -> {
				ctx.createPattern(this.bitmap.toHtmlNative().canvas, if (this.repeat) "repeat" else "no-repeat")
				//ctx.call("createPattern", this.bitmap.toHtmlNative().canvas)
			}
			else -> "black"
		}
	}

	inline private fun <T> keep(callback: () -> T): T {
		ctx.save()
		try {
			return callback()
		} finally {
			ctx.restore()
		}
	}

	private fun setFont(font: Context2d.Font) {
		ctx.font = "${font.size}px '${font.name}'"
	}

	private fun setState(state: Context2d.State, fill: Boolean) {
		ctx.globalAlpha = state.globalAlpha
		setFont(state.font)
		val t = state.transform
		ctx.setTransform(t.a, t.b, t.c, t.d, t.tx, t.ty)
		if (fill) {
			ctx.fillStyle = state.fillStyle.toJsStr()
		} else {
			ctx.lineWidth = state.lineWidth
			ctx.lineJoin = when (state.lineJoin) {
				Context2d.LineJoin.BEVEL -> CanvasLineJoin.BEVEL
				Context2d.LineJoin.MITER -> CanvasLineJoin.MITER
				Context2d.LineJoin.ROUND -> CanvasLineJoin.ROUND
			}
			ctx.lineCap = when (state.lineCap) {
				Context2d.LineCap.BUTT -> CanvasLineCap.BUTT
				Context2d.LineCap.ROUND -> CanvasLineCap.ROUND
				Context2d.LineCap.SQUARE -> CanvasLineCap.SQUARE
			}
			ctx.strokeStyle = state.strokeStyle.toJsStr()
		}
	}

	private fun transformPaint(paint: Context2d.Paint) {
		if (paint is Context2d.TransformedPaint) {
			val m = paint.transform
			ctx.transform(m.a, m.b, m.c, m.d, m.tx, m.ty)
		}
	}

	override fun drawImage(image: Bitmap, x: Int, y: Int, width: Int, height: Int, transform: Matrix2d) {
		ctx.save()
		try {
			transform.run { ctx.setTransform(a, b, c, d, tx, ty) }
			ctx.drawImage(
				(image.ensureNative() as CanvasNativeImage).canvas,
				x.toDouble(),
				y.toDouble(),
				width.toDouble(),
				height.toDouble()
			)
		} finally {
			ctx.restore()
		}
	}

	override fun render(state: Context2d.State, fill: Boolean) {
		if (state.path.isEmpty()) return

		//println("beginPath")
		keep {
			setState(state, fill)
			ctx.beginPath()

			state.path.visitCmds(
				moveTo = { x, y -> ctx.moveTo(x, y) },
				lineTo = { x, y -> ctx.lineTo(x, y) },
				quadTo = { cx, cy, ax, ay -> ctx.quadraticCurveTo(cx, cy, ax, ay) },
				cubicTo = { cx1, cy1, cx2, cy2, ax, ay -> ctx.bezierCurveTo(cx1, cy1, cx2, cy2, ax, ay) },
				close = { ctx.closePath() }
			)

			ctx.save()

			if (fill) {
				transformPaint(state.fillStyle)
				ctx.fill()
				//println("fill: $s")
			} else {
				transformPaint(state.strokeStyle)

				ctx.stroke()
				//println("stroke: $s")
			}

			ctx.restore()
		}
	}

	override fun renderText(
		state: Context2d.State,
		font: Context2d.Font,
		text: String,
		x: Double,
		y: Double,
		fill: Boolean
	) {
		keep {
			setState(state, fill)

			ctx.textBaseline = when (state.verticalAlign) {
				Context2d.VerticalAlign.TOP -> CanvasTextBaseline.TOP
				Context2d.VerticalAlign.MIDLE -> CanvasTextBaseline.MIDDLE
				Context2d.VerticalAlign.BASELINE -> CanvasTextBaseline.ALPHABETIC
				Context2d.VerticalAlign.BOTTOM -> CanvasTextBaseline.BOTTOM
			}
			ctx.textAlign = when (state.horizontalAlign) {
				Context2d.HorizontalAlign.LEFT -> CanvasTextAlign.LEFT
				Context2d.HorizontalAlign.CENTER -> CanvasTextAlign.CENTER
				Context2d.HorizontalAlign.RIGHT -> CanvasTextAlign.RIGHT
			}

			if (fill) {
				ctx.fillText(text, x, y)
			} else {
				ctx.strokeText(text, x, y)
			}
		}
	}

	override fun getBounds(font: Context2d.Font, text: String, out: Context2d.TextMetrics) {
		keep {
			setFont(font)
			val metrics = ctx.measureText(text)
			val width = metrics.width.toInt()
			out.bounds.setTo(0.toDouble(), 0.toDouble(), width.toDouble() + 2, font.size)
		}
	}
}