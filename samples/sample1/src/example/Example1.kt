package example

import com.soywiz.korio.*
import com.soywiz.korio.file.std.*

fun main(args: Array<String>): Unit = Korio {
    val vfs = MemoryVfsMix("hello" to "WORLD")
    println("HELLO ${vfs["hello"].readString()} FROM KORIO")
}


/*
fun main(args: Array<String>): Unit = Korge(MyModule)

object MyModule : Module() {
    override val mainScene = MyScene::class
    override suspend fun init(injector: AsyncInjector) {
        injector
            .mapPrototype { MyScene() }
    }
}

class MyScene : Scene() {
    override suspend fun sceneInit(sceneView: Container) {
    }
}
*/
