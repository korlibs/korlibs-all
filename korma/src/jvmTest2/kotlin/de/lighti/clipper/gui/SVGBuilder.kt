package de.lighti.clipper.gui

import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.clipper.Clipper.PolyFillType
import com.soywiz.korma.geom.clipper.Path
import com.soywiz.korma.geom.clipper.Paths
import java.awt.Color
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import java.util.*

//a very simple class that builds an SVG file with any number of
//polygons of the specified formats ...
class SVGBuilder {

	inner class PolyInfo {
		var polygons: Path? = null
		var si: StyleInfo? = null
	}

	inner class StyleInfo {
		var pft: PolyFillType
		var brushClr: Color
		var penClr: Color
		var penWidth: Double = 0.toDouble()
		var dashArray: IntArray? = null
		var showCoords: Boolean = false

		init {
			pft = PolyFillType.NON_ZERO
			brushClr = Color.WHITE
			dashArray = null
			penClr = Color.BLACK
			penWidth = 0.8
			showCoords = false
		}

		fun clone(): StyleInfo {
			val si = StyleInfo()
			si.pft = pft
			si.brushClr = brushClr
			si.dashArray = dashArray
			si.penClr = penClr
			si.penWidth = penWidth
			si.showCoords = showCoords
			return si
		}
	}

	////////////////////////////////////////////////

	////////////////////////////////////////////////

	////////////////////////////////////////////////
	var style: StyleInfo

	private val PolyInfoList: MutableList<PolyInfo>
	////////////////////////////////////////////////

	init {
		PolyInfoList = ArrayList<PolyInfo>()
		style = StyleInfo()
	}

	////////////////////////////////////////////////
	fun addPaths(poly: Paths) {
		if (poly.size == 0) {
			return
		}
		for (p in poly) {
			val pi = PolyInfo()
			pi.polygons = p
			pi.si = style.clone()
			PolyInfoList.add(pi)
		}
	}

	@Throws(IOException::class)
	fun saveToFile(filename: String, scale: Double): Boolean {
		return SaveToFile(filename, scale, 10)
	}

	@Throws(IOException::class)
	fun SaveToFile(filename: String): Boolean {
		return saveToFile(filename, 1.0)
	}

	@Throws(IOException::class)
	fun SaveToFile(filename: String, scale: Double, margin: Int): Boolean {
		var scale = scale
		var margin = margin
		//Temporarily set the locale to US to avoid decimal confusion
		//in SVG. Apparently has to be #.##
		val loc = Locale.getDefault()
		Locale.setDefault(Locale.US)

		if (scale == 0.0) {
			scale = 1.0
		}
		if (margin < 0) {
			margin = 0
		}

		//calculate the bounding rect ...
		var i = 0
		var j = 0
		while (i < PolyInfoList.size) {
			j = 0
			while (j < PolyInfoList[i].polygons!!.size && PolyInfoList[i].polygons!!.size == 0) {
				j++
			}
			if (j < PolyInfoList[i].polygons!!.size) {
				break
			}
			i++
		}
		if (i == PolyInfoList.size) {
			return false
		}
		val rec = Rectangle()
		rec.left = PolyInfoList[i].polygons!![j].x
		rec.right = rec.left
		rec.top = PolyInfoList[0].polygons!![j].y
		rec.bottom = rec.top

		while (i < PolyInfoList.size) {

			for ((x, y) in PolyInfoList[i].polygons!!) {
				if (x < rec.left) {
					rec.left = x
				} else if (x > rec.right) {
					rec.right = x
				}
				if (y < rec.top) {
					rec.top = y
				} else if (y > rec.bottom) {
					rec.bottom = y
				}
			}
			i++

		}

		rec.left = (rec.left * scale).toInt().toDouble()
		rec.top = (rec.top * scale).toInt().toDouble()
		rec.right = (rec.right * scale).toInt().toDouble()
		rec.bottom = (rec.bottom * scale).toInt().toDouble()
		val offsetX = -rec.left + margin
		val offsetY = -rec.top + margin

		val writer = BufferedWriter(FileWriter(filename))
		try {
			writer.write(
				String.format(
					SVG_HEADER,
					rec.right - rec.left + margin * 2,
					rec.bottom - rec.top + margin * 2,
					rec.right - rec.left + margin * 2,
					rec.bottom - rec.top + margin * 2
				)
			)

			for (pi in PolyInfoList) {
				writer.write(" <path d=\"")
				val p = pi.polygons
				if (p!!.size < 3) {
					continue
				}
				writer.write(String.format(" M %.2f %.2f", p[0].x * scale + offsetX, p[0].y * scale + offsetY))
				for (k in 1..p.size - 1) {
					writer.write(String.format(" L %.2f %.2f", p[k].x * scale + offsetX, p[k].y * scale + offsetY))
				}
				writer.write(" z")

				writer.write(
					String.format(
						SVG_PATH_FORMAT,
						Integer.toHexString(pi.si!!.brushClr.rgb and 0xffffff),
						pi.si!!.brushClr.alpha.toFloat() / 255,
						if (pi.si!!.pft === PolyFillType.EVEN_ODD) "evenodd" else "nonzero",
						Integer.toHexString(pi.si!!.penClr.rgb and 0xffffff),
						pi.si!!.penClr.alpha.toFloat() / 255,
						pi.si!!.penWidth
					)
				)

				if (pi.si!!.showCoords) {
					writer.write(String.format("<g font-family=\"Verdana\" font-size=\"11\" fill=\"black\">%n%n"))

					for ((x, y) in p) {
						writer.write(
							String.format(
								"<text x=\"%d\" y=\"%d\">%d,%d</text>\n",
								(x * scale + offsetX).toInt(),
								(y * scale + offsetY).toInt(),
								x,
								y
							)
						)

					}
					writer.write(String.format("%n"))

					writer.write(String.format("</g>%n"))
				}
			}
			writer.write(String.format("</svg>%n"))
			writer.close()
			return true

		} finally {
			//Reset locale
			Locale.setDefault(loc)
			writer.close()
		}
	}

	companion object {

		////////////////////////////////////////////////
		private val SVG_HEADER =
			"<?xml version=\"1.0\" standalone=\"no\"?>\n<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.0//EN\"\n\"http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd\">\n\n<svg width=\"%dpx\" height=\"%dpx\" viewBox=\"0 0 %d %d\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">\n\n"

		private val SVG_PATH_FORMAT =
			"\"%n style=\"fill:#%s; fill-opacity:%.2f; fill-rule:%s; stroke:#%s; stroke-opacity:%.2f; stroke-width:%.2f;\"/>%n%n"
	}
}