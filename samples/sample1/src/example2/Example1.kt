package example2

import com.dragonbones.model.*
import com.soywiz.klogger.*
import com.soywiz.korge.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.serialization.json.*

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
		val factory = DragonbonesFactory()
		val data = factory.parseDragonBonesData(Json.parse(resourcesRoot["Dragon/Dragon_ske.json"].readString())!!)
		val atlas = factory.parseTextureAtlasData(Json.parse(resourcesRoot["Dragon/Dragon_tex.json"].readString())!!, resourcesRoot["Dragon/Dragon_tex.png"].readBitmap(defaultImageFormats))
		checkData(data, atlas)
		val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")

		this += armatureDisplay
		/*
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
		*/
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

	private fun checkData(data: DragonBonesData?, atlas: TextureAtlasData) {
		println("$data, $atlas")
	}
}
