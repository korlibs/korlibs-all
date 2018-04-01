package com.soywiz.kmedialayer.scene

import com.soywiz.kmedialayer.*

interface SceneApplication {
    suspend fun changeScene(scene: Scene): Unit

    val kml: KmlBase
    val mouse: Mouse

    class Mouse {
        var x: Int = -1000; internal set
        var y: Int = -1000; internal set
        val buttons = (0 until 4).map { Button(it) }

        class Button(val id: Int) {
            var downX: Int = 0
            var downY: Int = 0
            var downTime: Double = 0.0; internal set

            var upX: Int = 0
            var upY: Int = 0
            var upTime: Double = 0.0; internal set

            var pressed: Boolean = false; internal set
        }

        fun button(index: Int) = buttons.getOrNull(index)
        fun pressing(button: Int) = button(button)?.pressed ?: false
    }

    class Gamepad {
        class Button(val id: Int) {
            var ratio: Float = 0f
            val pressed: Boolean get() = ratio >= 0.05
        }
        val buttons = (0 until 32).map { Button(it) }
    }
}

class DefaultSceneApplication(v: Boolean, val windowConfig: WindowConfig = WindowConfig(), val sceneGen: () -> Scene) :
    KMLWindowListener(), SceneApplication {
    lateinit var scene: Scene
    lateinit var renderContext: SceneRenderContext
    override val kml: KmlBase = Kml

    override suspend fun init(gl: KmlGl) = gl.run {
        renderContext = SceneRenderContext(SceneBatcher(gl, windowConfig.width, windowConfig.height))
        scene = sceneGen().apply { this.gl = gl; this.application = this@DefaultSceneApplication; init() }
        scene.apply { updateScene(0) }
        Unit
    }

    private var lastTime = 0.0
    override fun render(gl: KmlGl) {
        if (lastTime == 0.0) lastTime = kml.currentTimeMillis()
        val now = kml.currentTimeMillis()
        if (now != lastTime) {
            scene.apply {
                updateScene((now - lastTime).toInt())
            }

            lastTime = now
        }

        //super.render(gl)
        val bgcolor = windowConfig.bgcolor
        gl.clearColor(bgcolor[0], bgcolor[1], bgcolor[2], bgcolor[3])
        gl.clear(gl.COLOR_BUFFER_BIT)
        scene.render(renderContext)
        renderContext.flush()
    }

    override suspend fun changeScene(scene: Scene): Unit {
        this.scene = scene.apply { this.gl = gl; this.application = this@DefaultSceneApplication; init() }
        scene.apply { updateScene(0) }
        Unit
    }

    override fun keyUpdate(key: Key, pressed: Boolean) {
        scene.apply {
            if (pressed) {
                keyDown(key)
            } else {
                keyUp(key)
            }
        }
    }

    override fun gamepadConnection(player: Int, name: String, connected: Boolean) {
        scene.apply {
            _gameConnectionUpdate(player, name, connected)
        }
    }

    override fun gamepadButtonUpdate(player: Int, button: GameButton, ratio: Double) {
        scene.apply {
            _gameButtonUpdate(player, button, ratio)
        }
    }

    override fun gamepadStickUpdate(player: Int, stick: GameStick, x: Double, y: Double) {
        scene.apply {
            _gameStickUpdate(player, stick, x, y)
        }
    }

    override val mouse = SceneApplication.Mouse()


    override fun mouseUpdateMove(x: Int, y: Int) {
        scene.apply {
            mouse.x = x
            mouse.y = y
            mouseMoved(x, y)
        }
    }

    override fun mouseUpdateButton(button: Int, pressed: Boolean) {
        scene.apply {
            mouse.button(button)?.let { button ->
                val now = kml.currentTimeMillis()
                val changed = button.pressed != pressed
                button.pressed = pressed
                if (pressed) {
                    button.downX = mouse.x
                    button.downY = mouse.y
                    button.downTime = now
                } else {
                    button.upX = mouse.x
                    button.upY = mouse.y
                    button.upTime = now
                }
                if (changed) {
                    if (pressed) {
                        //println("DOWN")
                        mouseDown(button.id)
                    } else {
                        //println("UP")
                        val elapsed = now - button.downTime
                        val movedX = mouse.x - button.downX
                        val movedY = mouse.y - button.downY
                        mouseUp(button.id)
                        //println("$elapsed, $movedX, $movedY")
                        if (elapsed <= 250.0 && (movedX < 16) && (movedY < 16)) {
                            //println("CLICK!")
                            mouseClick(button.id)
                        }
                    }
                }
            }
        }
    }

    override fun resized(width: Int, height: Int) {
        super.resized(width, height)
        KmlGlUtil.ortho(width, height, 0f, 1f, renderContext.batcher.ortho)
    }
}

fun SceneApplication(windowConfig: WindowConfig = WindowConfig(), sceneGen: () -> Scene) {
    SceneScope.apply {
        Kml.application(windowConfig, DefaultSceneApplication(true, windowConfig, sceneGen))
    }
}
