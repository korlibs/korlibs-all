package com.soywiz.korge.view

import com.soywiz.korge.bitmapfont.*
import com.soywiz.korge.html.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.*
import com.soywiz.korma.geom.*

interface IText {
	var text: String
}

interface IHtml {
	var html: String
}

class Text : View(), IText, IHtml {
	companion object {
	    operator fun invoke(text: String, textSize: Double = 16.0, color: Int = Colors.WHITE, font: BitmapFont = Fonts.defaultFont): Text = Text().apply {
			this.format = Html.Format(color = color, face = Html.FontFace.Bitmap(font), size = textSize.toInt())
			if (text != "") this.text = text
		}
	}

	//var verticalAlign: Html.VerticalAlignment = Html.VerticalAlignment.TOP
	val textBounds = Rectangle(0, 0, 1024, 1024)
	private val tempRect = Rectangle()
	var _text: String = ""
	var _html: String = ""
	var document: Html.Document? = null
	private var _format: Html.Format = Html.Format()
	var filtering = true
	var autoSize = true
		set(value) {
			field = value
			recalculateBoundsWhenRequired()
		}
	var bgcolor = Colors.TRANSPARENT_BLACK
	val fonts = Fonts.fonts

	fun setTextBounds(rect: Rectangle) {
		this.textBounds.copyFrom(rect)
		autoSize = false
	}


	fun unsetTextBounds() {
		autoSize = true
	}

	var format: Html.Format
		get() = _format
		set(value) {
			_format = value
			if (value != document?.defaultFormat) {
				document?.defaultFormat?.parent = value
			}
			recalculateBoundsWhenRequired()
		}

	override var text: String
		get() = if (document != null) document?.xml?.text ?: "" else _text
		set(value) {
			_text = value
			_html = ""
			document = null
			recalculateBoundsWhenRequired()
		}
	override var html: String
		get() = if (document != null) _html else _text
		set(value) {
			document = Html.parse(value)
			relayout()
			document!!.defaultFormat.parent = format
			_text = ""
			_html = value
			_format = document!!.firstFormat.consolidate()
		}

	fun relayout() {
		document?.doPositioning(fonts, textBounds)
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		val colorMul = globalColorMul
		val colorAdd = globalColorAdd
		if (document != null) {
			for (span in document!!.allSpans) {
				val font = fonts.getBitmapFont(span.format)
				val format = span.format
				font.drawText(
					ctx, format.computedSize.toDouble(), text,
					span.bounds.x.toInt(), span.bounds.y.toInt(),
					m,
					colMul = RGBA.multiply(colorMul, format.computedColor),
					colAdd = colorAdd,
					blendMode = computedBlendMode,
					filtering = filtering
				)
			}
		} else {
			val font = fonts.getBitmapFont(format)
			val anchor = format.computedAlign.anchor
			fonts.getBounds(text, format, out = tempRect)
			//println("tempRect=$tempRect, textBounds=$textBounds")
			//tempRect.setToAnchoredRectangle(tempRect, format.align.anchor, textBounds)
			//val x = (textBounds.width) * anchor.sx - tempRect.width
			val px = textBounds.x + (textBounds.width - tempRect.width) * anchor.sx
			//val x = textBounds.x + (textBounds.width) * anchor.sx
			val py = textBounds.y + (textBounds.height - tempRect.height) * anchor.sy

			if (RGBA.getA(bgcolor) != 0) {
				ctx.batch.drawQuad(
					ctx.getTex(Bitmaps.white),
					x = textBounds.x.toFloat(),
					y = textBounds.y.toFloat(),
					width = textBounds.width.toFloat(),
					height = textBounds.height.toFloat(),
					m = m,
					filtering = false,
					colorMul = RGBA.multiply(bgcolor, globalColorMul),
					colorAdd = colorAdd,
					blendFactors = computedBlendMode.factors
				)
			}

			//println(" -> ($x, $y)")
			font.drawText(
				ctx, format.computedSize.toDouble(), text, px.toInt(), py.toInt(),
				m,
				colMul = RGBA.multiply(colorMul, format.computedColor),
				colAdd = colorAdd,
				blendMode = computedBlendMode,
				filtering = filtering
			)
		}
	}

	private fun recalculateBounds() {
		fonts.getBounds(text, format, out = textBounds)
	}

	private fun recalculateBoundsWhenRequired() {
		if (autoSize) recalculateBounds()
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		if (document != null) {
			out.copyFrom(document!!.bounds)
		} else {
			if (autoSize) {
				fonts.getBounds(text, format, out)
				out.setToAnchoredRectangle(out, format.computedAlign.anchor, textBounds)
			} else {
				out.copyFrom(textBounds)
			}
			//println(textBounds)
		}
	}

	override fun createInstance(): View = Text()
	override fun copyPropsFrom(source: View) {
		super.copyPropsFrom(source)
		source as Text
		this.textBounds.copyFrom(source.textBounds)
		if (source._html.isNotEmpty()) {
			this.html = source.html
		} else {
			this.text = source.text
		}
	}
}

fun Views.text(text: String, textSize: Double = 16.0, color: Int = Colors.WHITE, font: BitmapFont = Fonts.defaultFont) =
	Text().apply {
		this.format = Html.Format(color = color, face = Html.FontFace.Bitmap(font), size = textSize.toInt())
		if (text != "") this.text = text
	}

fun Container.text(text: String, textSize: Double = 16.0, font: BitmapFont = Fonts.defaultFont): Text =
	text(text, textSize, font) {
	}

inline fun Container.text(
	text: String,
	textSize: Double = 16.0,
	font: BitmapFont = Fonts.defaultFont,
	callback: Text.() -> Unit
): Text {
	val child = Text(text, textSize = textSize, font = font)
	this += child
	callback(child)
	return child
}

fun View?.setText(text: String) {
	this.foreachDescendant {
		if (it is IText) it.text = text
	}
}

fun View?.setHtml(html: String) {
	this.foreachDescendant {
		if (it is IHtml) it.html = html
	}
}
