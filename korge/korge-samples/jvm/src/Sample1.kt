import com.soywiz.kds.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.effect.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.*

object Sample1 {
	@JvmStatic
	fun main(args: Array<String>) = Korge(title = "Sample1") {
		//waveEffectView {
		//colorMatrixEffectView(ColorMatrixEffectView.GRAYSCALE_MATRIX) {
		//convolute3EffectView(Convolute3EffectView.KERNEL_EDGE_DETECTION) {
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
		//val bmp = SolidRect(100, 100, Colors.RED).renderToBitmap(views)
		//val bmp = view.renderToBitmap(views)
		//bmp.writeTo("/tmp/demo.png".uniVfs, defaultImageFormats)
		//println(bmp)
	}
}
