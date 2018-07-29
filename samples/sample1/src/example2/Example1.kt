package example2

import com.dragonbones.model.*
import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korma.geom.*
import kotlin.math.*

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
    //Korge(MyModule, debug = true)
    Korge(MyModule, debug = false)
}

object MyModule : Module() {
    override val mainScene = MyScene::class
    override suspend fun init(injector: AsyncInjector) {
        injector
            .mapPrototype { MyScene() }
    }

    override val size: SizeInt = SizeInt(800, 600)
    override val windowSize: SizeInt = SizeInt(800, 600)
}

class MyScene : Scene() {
    val _animationNames = listOf(
        "PARAM_ANGLE_X",
        "PARAM_ANGLE_Y",
        "PARAM_ANGLE_Z",
        "PARAM_EYE_BALL_X",
        "PARAM_EYE_BALL_Y",
        "PARAM_BODY_X",
        "PARAM_BODY_Y",
        "PARAM_BODY_Z",
        "PARAM_BODY_ANGLE_X",
        "PARAM_BODY_ANGLE_Y",
        "PARAM_BODY_ANGLE_Z",
        "PARAM_BREATH"
    )

    var totalTime = 0.0

    override suspend fun Container.sceneInit() {
        //DragonBones.debugDraw = true
        val factory = KorgeDbFactory()

        this.x = views.actualVirtualWidth.toDouble() / 2.0
        this.y = views.actualVirtualHeight.toDouble() / 2.0

        //run {
        //    val scale = 0.3
        //    factory.parseDragonBonesData(
        //        Json.parse(resourcesRoot["you_xin/body/body_ske.json"].readString())!!,
        //        "you_xin"
        //    )
        //        val atlas = factory.parseTextureAtlasData(
        //            Json.parse(resourcesRoot["you_xin/body/body_tex.json"].readString())!!,
        //            resourcesRoot["you_xin/body/body_tex.png"].readBitmapOptimized()
        //        )
        //    val armatureDisplay = factory.buildArmatureDisplay("body", "you_xin")!!.position(0, 200).scale(scale)
        //    this += armatureDisplay
        //
        //    println(armatureDisplay.animation.animationNames)
        //    //armatureDisplay.animation.play("idle_00")
        //}

        run {
            val scale = 0.3
            factory.parseDragonBonesData(
                Json.parse(resourcesRoot["shizuku/shizuku_ske.json"].readString())!!,
                "shizuku"
            )
            factory.updateTextureAtlases(arrayOf(
                resourcesRoot["shizuku/shizuku.1024/texture_00.png"].readBitmapOptimized().mipmaps(),
                resourcesRoot["shizuku/shizuku.1024/texture_01.png"].readBitmapOptimized().mipmaps(),
                resourcesRoot["shizuku/shizuku.1024/texture_02.png"].readBitmapOptimized().mipmaps(),
                resourcesRoot["shizuku/shizuku.1024/texture_03.png"].readBitmapOptimized().mipmaps()
            ), "shizuku")
            val armatureDisplay = factory.buildArmatureDisplay("shizuku", "shizuku")!!.position(0, 200).scale(scale)
            this += armatureDisplay

            println(armatureDisplay.animation.animationNames)
            //armatureDisplay.play("idle_00")
            armatureDisplay.animation.play("idle_00")

            val target = MPoint()

            mouse {
                moveAnywhere {
                    val mx = localMouseX(views)
                    val my = localMouseY(views)
                    //target.x = ((mx - this@sceneInit.x - armatureDisplay.x) / scale)
                    //target.y = ((my - this@sceneInit.y - armatureDisplay.y) / scale)
                    target.x = (mx - armatureDisplay.x) / scale
                    target.y = (my - armatureDisplay.y) / scale
                    //println("target:$target")
                }
            }

            addUpdatable {
                totalTime += it
                //val x = containerRoot.globalToLocalX(views.input.mouse.x, views.input.mouse.y) / scale
                //val y = containerRoot.globalToLocalY(views.input.mouse.x, views.input.mouse.y) / scale

                //target.x = x
                //target.y = y

                val armature = armatureDisplay.armature
                val animation = armatureDisplay.animation
                val canvas = armature.armatureData.canvas!!

                var p = 0.0
                val pX = max(min((target.x - canvas.x) / (canvas.width * 0.5), 1.0), -1.0)
                val pY = -max(min((target.y - canvas.y) / (canvas.height * 0.5), 1.0), -1.0)
                for (animationName in _animationNames) {
                    if (!animation.hasAnimation(animationName)) {
                        continue
                    }

                    var animationState = animation.getState(animationName, 1)
                    if (animationState == null) {
                        animationState = animation.fadeIn(animationName, 0.1, 1, 1, animationName)
                        if (animationState != null) {
                            animationState.resetToPose = false
                            animationState.stop()
                        }
                    }

                    if (animationState == null) {
                        continue
                    }

                    when (animationName) {
                        "PARAM_ANGLE_X", "PARAM_EYE_BALL_X" -> p = (pX + 1.0) * 0.5
                        "PARAM_ANGLE_Y", "PARAM_EYE_BALL_Y" -> p = (pY + 1.0) * 0.5
                        "PARAM_ANGLE_Z" -> p = (-pX * pY + 1.0) * 0.5
                        "PARAM_BODY_X", "PARAM_BODY_ANGLE_X" -> p = (pX + 1.0) * 0.5
                        "PARAM_BODY_Y", "PARAM_BODY_ANGLE_Y" -> p = (-pX * pY + 1.0) * 0.5
                        "PARAM_BODY_Z", "PARAM_BODY_ANGLE_Z" -> p = (-pX * pY + 1.0) * 0.5
                        "PARAM_BREATH" -> p = (sin(totalTime / 1000.0) + 1.0) * 0.5
                    }

                    animationState.currentTime = p * animationState.totalTime
                }
            }
        }

        //val data = factory.parseDragonBonesData(Json.parse(resourcesRoot["Dragon/Dragon_ske.json"].readString())!!)
        //val atlas = factory.parseTextureAtlasData(
        //    Json.parse(resourcesRoot["Dragon/Dragon_tex.json"].readString())!!,
        //    resourcesRoot["Dragon/Dragon_tex.png"].readBitmapOptimized()
        //)

        //run {
        //    //val scale = 0.3
        //    val scale = 0.8
        //    val data = factory.parseDragonBonesData(Json.parse(resourcesRoot["Dragon/Dragon_ske.json"].readString())!!)
        //    val atlas = factory.parseTextureAtlasData(
        //        Json.parse(resourcesRoot["Dragon/Dragon_tex.json"].readString())!!,
        //        resourcesRoot["Dragon/Dragon_tex.png"].readBitmapOptimized()
        //    )
        //    val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")!!.position(0, 200).scale(scale)
        //    armatureDisplay.animation.play("walk")
        //    println(armatureDisplay.animation.animationNames)
        //    //armatureDisplay.animation.play("jump")
        //    //armatureDisplay.animation.play("fall")
        //    this += armatureDisplay
        //}

        //run {
        //    val scale = 0.8
        //    val data = factory.parseDragonBonesData(Json.parse(resourcesRoot["mecha_1002_101d_show/mecha_1002_101d_show_ske.json"].readString())!!)
        //    val atlas = factory.parseTextureAtlasData(
        //        Json.parse(resourcesRoot["mecha_1002_101d_show/mecha_1002_101d_show_tex.json"].readString())!!,
        //        resourcesRoot["mecha_1002_101d_show/mecha_1002_101d_show_tex.png"].readBitmapOptimized()
        //    )
        //    val armatureDisplay = factory.buildArmatureDisplay("mecha_1002_101d")!!.position(0, 200).scale(scale)
        //    //armatureDisplay.animation.play("walk")
        //    println(armatureDisplay.animation.animationNames)
        //    //armatureDisplay.animation.play("jump")
        //    armatureDisplay.animation.play("idle")
        //    this += armatureDisplay
        //}

        //addUpdatable {
        //    factory.clock.advanceTime(it.toDouble() / 1000.0)
        //}
        /*
        //armatureDisplay.animation.play("pinch_in_02")

        */

        //checkData(data, atlas)

        //val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")!!.position(300, 300)
        //val armatureDisplay = factory.buildArmatureDisplay("NewDragon", "NewDragon")!!.position(300, 300)

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

        //this.containerRoot.dump()

        //println(armatureDisplay.animation.animationNames)
        //armatureDisplay.animation.gotoAndPlayByTime("walk")
        //println(atlas.textures.keys)
        //image((atlas.getTexture("parts/tailTip") as KorgeDbTextureData).renderTexture!!)

        //solidRect(50, 50, Colors.RED)

        //this += armatureDisplay
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


        //launchImmediately {
        //    delay(0.3.seconds)
        //    this.containerRoot.dump()
        //    //this.alpha = 0.3
        //}

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
