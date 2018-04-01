import com.soywiz.korge.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*

object MainTest {
	@JvmStatic
	fun main(args: Array<String>) = Korge(object : Module() {
		override val mainScene = TestMainScene::class.java
	})
}

class TestMainScene : Scene() {
	suspend override fun sceneInit(sceneView: Container) {
	}
}
