import com.soywiz.korge.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korio.*

object MainTest {
	@JvmStatic
	fun main(args: Array<String>) = Korge(object : Module() {
		override val mainScene = TestMainScene::class
	})
}

class TestMainScene : Scene() {
	override suspend fun sceneInit(sceneView: Container) {
	}
}
