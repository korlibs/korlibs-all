package example2

import com.dragonbones.event.*
import com.soywiz.kds.*
import com.soywiz.korge.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.random.*
import com.soywiz.korui.light.*
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
    override val quality: LightQuality = LightQuality.QUALITY

    override suspend fun init(injector: AsyncInjector) {
        injector
            .mapPrototype { MyScene() }
            .mapPrototype { ClassicDragonScene() }
            .mapPrototype { EyeTrackingScene() }
            .mapPrototype { HelloWorldScene() }
            .mapPrototype { SkinChangingScene() }
    }

    override val size: SizeInt = SizeInt(1280, 720)
    override val windowSize: SizeInt = SizeInt(1280, 720)
}

class MyScene : Scene() {
    lateinit var buttonContainer: Container

    override suspend fun Container.sceneInit() {
        val mySceneContainer = sceneContainer(views) {
            this.x = views.virtualWidth.toDouble() * 0.5
            this.y = views.virtualHeight.toDouble() * 0.5
        }
        buttonContainer = this
        this += Button("Hello") { mySceneContainer.changeToDisablingButtons<HelloWorldScene>() }.position(8, views.virtualHeight - 48)
        //this += Button("Classic") { mySceneContainer.changeToDisablingButtons<ClassicDragonScene>() }.position(108, views.virtualHeight - 48)
        this += Button("Eye Tracking") { mySceneContainer.changeToDisablingButtons<EyeTrackingScene>() }.position(150, views.virtualHeight - 48)
        this += Button("Skin Changing") { mySceneContainer.changeToDisablingButtons<SkinChangingScene>() }.position(410, views.virtualHeight - 48)
        mySceneContainer.changeToDisablingButtons<HelloWorldScene>()
    }

    suspend inline fun <reified T : Scene> SceneContainer.changeToDisablingButtons() {
        for (child in buttonContainer.children.filterIsInstance<Button>()) child.enabledButton = false
        try {
            changeTo<T>()
        } finally {
            for (child in buttonContainer.children.filterIsInstance<Button>()) child.enabledButton = true
        }
    }
}

class Button(text: String, handler: suspend () -> Unit) : Container() {
    val textField = Text(text, textSize = 32.0)
    private val bounds = textField.textBounds
    val g = Graphics().apply {
        fill(Colors.DARKGREY, 0.7) {
            drawRoundRect(bounds.x, bounds.y, bounds.width + 16, bounds.height + 16, 8.0, 8.0)
        }
    }
    var enabledButton = true
        set(value) {
            field = value
            updateState()
        }
    private var overButton = false
        set(value) {
            field = value
            updateState()
        }

    fun updateState() {
        when {
            !enabledButton -> alpha = 0.3
            overButton -> alpha = 1.0
            else -> alpha = 0.8
        }
    }

    init {
        //this += this.solidRect(bounds.width, bounds.height, Colors.TRANSPARENT_BLACK)
        this += g.apply {
            mouseEnabled = true
        }
        this += textField.position(8, 8)

        mouse {
            over { overButton = true }
            out { overButton = false }
        }
        onClick { if (enabledButton) handler() }
        updateState()
    }
}

class HelloWorldScene : BaseDbScene() {
    val SCALE = 1.6
    override suspend fun Container.sceneInit() {
        val data =
            factory.parseDragonBonesData(Json.parse(resourcesRoot["mecha_1002_101d_show/mecha_1002_101d_show_ske.json"].readString())!!)
        val atlas = factory.parseTextureAtlasData(
            Json.parse(resourcesRoot["mecha_1002_101d_show/mecha_1002_101d_show_tex.json"].readString())!!,
            resourcesRoot["mecha_1002_101d_show/mecha_1002_101d_show_tex.png"].readBitmapOptimized()
        )
        val armatureDisplay = factory.buildArmatureDisplay("mecha_1002_101d")!!
            .position(0, 300).scale(SCALE)
        //armatureDisplay.animation.play("walk")
        println(armatureDisplay.animation.animationNames)
        //armatureDisplay.animation.play("jump")
        armatureDisplay.animation.play("idle")
        this += armatureDisplay
    }
}

class ClassicDragonScene : BaseDbScene() {
    override suspend fun Container.sceneInit() {
        //val scale = 0.3
        val scale = 0.8
        val data = factory.parseDragonBonesData(Json.parse(resourcesRoot["Dragon/Dragon_ske.json"].readString())!!)
        val atlas = factory.parseTextureAtlasData(
            Json.parse(resourcesRoot["Dragon/Dragon_tex.json"].readString())!!,
            resourcesRoot["Dragon/Dragon_tex.png"].readBitmapOptimized()
        )
        val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")!!.position(0, 200).scale(scale)
        armatureDisplay.animation.play("walk")
        println(armatureDisplay.animation.animationNames)
        //armatureDisplay.animation.play("jump")
        //armatureDisplay.animation.play("fall")
        this += armatureDisplay
    }
}


class EyeTrackingScene : BaseDbScene() {
    val scale = 0.46
    var totalTime = 0.0

    override suspend fun Container.sceneInit() {
        val _animationNames = listOf(
            "PARAM_ANGLE_X", "PARAM_ANGLE_Y", "PARAM_ANGLE_Z",
            "PARAM_EYE_BALL_X", "PARAM_EYE_BALL_Y",
            "PARAM_BODY_X", "PARAM_BODY_Y", "PARAM_BODY_Z",
            "PARAM_BODY_ANGLE_X", "PARAM_BODY_ANGLE_Y", "PARAM_BODY_ANGLE_Z",
            "PARAM_BREATH"
        )
        factory.parseDragonBonesData(
            Json.parse(resourcesRoot["shizuku/shizuku_ske.json"].readString())!!,
            "shizuku"
        )
        factory.updateTextureAtlases(
            arrayOf(
                resourcesRoot["shizuku/shizuku.1024/texture_00.png"].readBitmapOptimized().mipmaps(),
                resourcesRoot["shizuku/shizuku.1024/texture_01.png"].readBitmapOptimized().mipmaps(),
                resourcesRoot["shizuku/shizuku.1024/texture_02.png"].readBitmapOptimized().mipmaps(),
                resourcesRoot["shizuku/shizuku.1024/texture_03.png"].readBitmapOptimized().mipmaps()
            ), "shizuku"
        )
        val armatureDisplay = factory.buildArmatureDisplay("shizuku", "shizuku")!!
            .position(0, 300).scale(this@EyeTrackingScene.scale)
        this += armatureDisplay

        println(armatureDisplay.animation.animationNames)
        //armatureDisplay.play("idle_00")
        armatureDisplay.animation.play("idle_00")

        val target = MPoint()

        mouse {
            moveAnywhere {
                target.x = (localMouseX(views) - armatureDisplay.x) / this@EyeTrackingScene.scale
                target.y = (localMouseY(views) - armatureDisplay.y) / this@EyeTrackingScene.scale
            }
        }

        addUpdatable {
            totalTime += it

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
}

class SkinChangingScene : BaseDbScene() {
    val SCALE = 0.42
    val random = Rand()

    override suspend fun Container.sceneInit() {
        val suitConfigs = listOf(
            listOf(
                "2010600a", "2010600a_1",
                "20208003", "20208003_1", "20208003_2", "20208003_3",
                "20405006",
                "20509005",
                "20703016", "20703016_1",
                "2080100c",
                "2080100e", "2080100e_1",
                "20803005",
                "2080500b", "2080500b_1"
            ),
            listOf(
                "20106010",
                "20106010_1",
                "20208006",
                "20208006_1",
                "20208006_2",
                "20208006_3",
                "2040600b",
                "2040600b_1",
                "20509007",
                "20703020",
                "20703020_1",
                "2080b003",
                "20801015"
            )
        )

        factory.parseDragonBonesData(
            Json.parse(resourcesRoot["you_xin/body/body_ske.json"].readString())!!
        )
        val atlas = factory.parseTextureAtlasData(
            Json.parse(resourcesRoot["you_xin/body/body_tex.json"].readString())!!,
            resourcesRoot["you_xin/body/body_tex.png"].readBitmapOptimized()
        )

        for ((i, suitConfig) in suitConfigs.withIndex()) {
            for (partArmatureName in suitConfig) {
                // resource/you_xin/suit1/2010600a/xxxxxx
                val path = "you_xin/" + "suit" + (i + 1) + "/" + partArmatureName + "/" + partArmatureName
                val dragonBonesJSONPath = path + "_ske.json"
                val textureAtlasJSONPath = path + "_tex.json"
                val textureAtlasPath = path + "_tex.png"
                //
                factory.parseDragonBonesData(Json.parse(resourcesRoot[dragonBonesJSONPath].readString())!!)
                factory.parseTextureAtlasData(
                    Json.parse(resourcesRoot[textureAtlasJSONPath].readString())!!,
                    resourcesRoot[textureAtlasPath].readBitmapOptimized()
                )
            }
        }

        val armatureDisplay = factory.buildArmatureDisplay("body")!!
            .position(0, 360).scale(SCALE)
        this += armatureDisplay

        println(armatureDisplay.animation.animationNames)
        //armatureDisplay.animation.play("idle_00")
        armatureDisplay.on(EventObject.LOOP_COMPLETE) {
            //println("LOOP!")
            // Random animation index.
            val nextAnimationName = random[armatureDisplay.animation.animationNames]
            armatureDisplay.animation.fadeIn(nextAnimationName, 0.3, 0)
        }
        armatureDisplay.animation.play("idle", 0)
        //armatureDisplay.animation.play("speak")

        for (part in suitConfigs[0]) {
            val partArmatureData = factory.getArmatureData(part)
            factory.replaceSkin(armatureDisplay.armature, partArmatureData!!.defaultSkin!!)
        }
        val _replaceSuitParts = arrayListOf<String>()
        var _replaceSuitIndex = 0

        mouse {
            onUpAnywhere {
                // This suit has been replaced, next suit.
                if (_replaceSuitParts.size == 0) {
                    _replaceSuitIndex++

                    if (_replaceSuitIndex >= suitConfigs.size) {
                        _replaceSuitIndex = 0
                    }

                    // Refill the unset parits.
                    for (partArmatureName in suitConfigs[_replaceSuitIndex]) {
                        _replaceSuitParts.add(partArmatureName)
                    }
                }

                // Random one part in this suit.
                val partIndex: Int = floor(random.nextDouble() * _replaceSuitParts.size).toInt()
                val partArmatureName = _replaceSuitParts[partIndex]
                val partArmatureData = factory.getArmatureData(partArmatureName)
                // Replace skin.
                factory.replaceSkin(armatureDisplay.armature, partArmatureData!!.defaultSkin!!)
                // Remove has been replaced
                _replaceSuitParts.splice(partIndex, 1)
            }
        }
    }
}

abstract class BaseDbScene : Scene() {
    val factory = KorgeDbFactory()
}
