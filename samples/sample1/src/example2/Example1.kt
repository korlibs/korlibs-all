package example2

import com.soywiz.klogger.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korinject.*

//fun main(args: Array<String>): Unit = Korio {
//    val vfs = MemoryVfsMix("hello" to "WORLD")
//    println("HELLO ${vfs["hello"].readString()} FROM KORIO")
//}

fun main(args: Array<String>): Unit {
	//Logger.defaultLevel = Logger.Level.TRACE
	//Logger("Views").level = Logger.Level.TRACE
	//Logger("Korge").level = Logger.Level.TRACE
	//Logger("RenderContext").level = Logger.Level.TRACE
	//Logger("BatchBuilder2D").level = Logger.Level.TRACE
	//Logger("DefaultShaders").level = Logger.Level.TRACE
	//Logger("RenderContext2D").level = Logger.Level.TRACE
	Korge(MyModule, debug = true)
}

object MyModule : Module() {
	override val mainScene = MyScene::class
	override suspend fun init(injector: AsyncInjector) {
		injector
			.mapPrototype { MyScene() }
	}
}

class MyScene : Scene() {
	override suspend fun Container.sceneInit() {
		graphics() {
			beginFill(Colors.RED, 1.0)
			//drawRect(0.0, 0.0, 128.0, 128.0)
			drawCircle(64.0, 64.0, 64.0)
			endFill()

			alpha = 0.5
			mouse {
				//hitTestType = View.HitTestType.SHAPE
				onOver { alpha = 1.0 }
				onOut { alpha = 0.5 }
			}
		}
		/*
		solidRect(128, 128, Colors.RED) {
			alpha = 0.5
			mouse {
				onOver { alpha = 1.0 }
				onOut { alpha = 0.5 }
			}
		}
		*/
	}
}
