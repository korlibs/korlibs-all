package example2

import com.dragonbones.model.*
import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korma.geom.*

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

    override val size: SizeInt = SizeInt(2048, 2048)
    override val windowSize: SizeInt = SizeInt(768, 768)
}

class MyScene : Scene() {

    override suspend fun Container.sceneInit() {
        //DragonBones.debugDraw = true
        val factory = KorgeDbFactory()
        val data = factory.parseDragonBonesData(Json.parse(resourcesRoot["Dragon/Dragon_ske.json"].readString())!!)
        val atlas = factory.parseTextureAtlasData(
            Json.parse(resourcesRoot["Dragon/Dragon_tex.json"].readString())!!,
            //resourcesRoot["Dragon/Dragon_tex.png"].readBitmapOptimized().toBMP32()
            resourcesRoot["Dragon/Dragon_tex.png"].readBitmapOptimized()
        )
        checkData(data, atlas)
        val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")!!.position(300, 300)
        //val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")!!.position(150, 150)
        /*
        for (tex in atlas.textures.entries) {
            val bmp = ((tex.value as DragonbonesTextureData).renderTexture!! as BitmapSlice<Bitmap>)
            JailedLocalVfs("/tmp/")[tex.key + ".tga"].ensureParents().writeBitmap(bmp.extract().toBMP32(), TGA)
        }
        armatureDisplay.mouse {
            click {
                println("CLICK!")
            }
        }
        */

        //image(Bitmaps.transparent) {
        //    position(150, 150)
        //    solidRect(100, 100, Colors.RED) {
        //        mouse {
        //            click {
        //                println("click!")
        //            }
        //        }
        //    }
        //}

        this.containerRoot.dump()

        println(armatureDisplay.animation.animationNames)
        armatureDisplay.animation.gotoAndPlayByTime("walk")
        println(atlas.textures.keys)
        //image((atlas.getTexture("parts/tailTip") as KorgeDbTextureData).renderTexture!!)

        //solidRect(50, 50, Colors.RED)

        this += armatureDisplay
        //armatureDisplay.apply {
        //    image((atlas.getTexture("parts/tailTip") as KorgeDbTextureData).renderTexture!!)
        //    val tex = this@sceneInit["parts/handL"] as Image
//
        //    //val hand = image((atlas.getTexture("parts/handL") as KorgeDbTextureData).renderTexture!!) {
        //    val hand = image(tex.bitmap) {
        //        //Image:pos=(-179.96,179.01):scale=(1,1):name=(parts/handL):bitmap=BitmapSlice(parts/handL:SizeInt(width=96, height=78))
//
        //        position(-179.96,179.01).scale(1.001, 1.001)
        //        name = "parts/handL"
        //    }
        //    this.addChildAt(hand, 0)
        //}

        //solidRect(30, 30, Colors.BLUE)


        this.containerRoot.dump()
        launchImmediately {
            delay(0.3.seconds)
            this.containerRoot.dump()
            //this.alpha = 0.3
        }

        /*
        timer(1.seconds) {
            this.containerRoot.dump()
        }
        */

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
