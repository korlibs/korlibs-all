package com.soywiz.kmedialayer.scene

import com.soywiz.kmedialayer.scene.components.moveBy
import kotlin.test.Test
import kotlin.test.assertEquals

class TestTestSceneApplication {
    @Test
    fun name() = testSceneApplication(MyScene()) { application ->
        val image = root["image"] as Image
        assertEquals(32f, image.tex.fwidth)
        assertEquals(32f, image.tex.fheight)
        assertEquals("0.0,0.0", "${image.x},${image.y}")
        moveImage()
        assertEquals("10.0,10.0", "${image.x},${image.y}")
        assertEquals(2016L, application.time)
    }

    class MyScene : Scene() {
        override suspend fun init() {
            root += Image(texture("mini.png")).apply {
                name = "image"
            }
        }

        suspend fun moveImage() {
            root["image"]?.moveBy(5.0, 5.0, time = 1.0)
            root["image"]?.moveBy(5.0, 5.0, time = 1.0)
        }
    }
}