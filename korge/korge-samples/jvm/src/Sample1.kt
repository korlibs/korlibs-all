import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.effect.*
import com.soywiz.korim.color.*

object Sample1 {
	@JvmStatic
	fun main(args: Array<String>) = Korge(title = "Sample1") {
		swizzleColorsEffectView("bgra") {
			x = 100.0
			y = 100.0
			solidRect(100, 100, Colors.RED) {
			}
		}
	}
}