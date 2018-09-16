import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.effect.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*

object Sample1 {
	@JvmStatic
	fun main(args: Array<String>) = Korge(title = "Sample1") {
		//waveEffectView {
		//colorMatrixEffectView(ColorMatrixEffectView.GRAYSCALE_MATRIX) {
		//convolute3EffectView(Convolute3EffectView.KERNEL_EDGE_DETECTION) {
		/*
		blurEffectView(radius = 1.0) {
			convolute3EffectView(Convolute3EffectView.KERNEL_GAUSSIAN_BLUR) {
				//convolute3EffectView(Convolute3EffectView.KERNEL_BOX_BLUR) {
				swizzleColorsEffectView("bgra") {
					x = 100.0
					y = 100.0
					image(Bitmap32(100, 100) { x, y -> RGBA(156 + x, 156 + y, 0, 255) })
					//solidRect(100, 100, Colors.RED)
				}
				//}
			}
		}.apply {
			tween(this::radius[10.0], time = 5.seconds)
		}
		*/

		val mfilter = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX, 0.0)
		image(Bitmap32(100, 100) { x, y -> RGBA(156 + x, 156 + y, 0, 255) }) {
			x = 100.0
			y = 100.0
			//filter = ComposedFilter(SwizzleColorsFilter("bgra"), SwizzleColorsFilter("bgra"))
			//filter = Convolute3Filter(Convolute3Filter.KERNEL_EDGE_DETECTION)
			filter = mfilter
		}.apply {
			tween(mfilter::blendRatio[0.0, 1.0], time = 4.seconds)
		}
		//val bmp = SolidRect(100, 100, Colors.RED).renderToBitmap(views)
		//val bmp = view.renderToBitmap(views)
		//bmp.writeTo("/tmp/demo.png".uniVfs, defaultImageFormats)
		//println(bmp)
	}
}
