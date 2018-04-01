package com.soywiz.korge.ext.swf

import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.vfs.*

class SwfTestDisabled {
	companion object {
		@JvmStatic
		fun main(args: Array<String>) = Korge(object : Module() {
			override val width: Int = (550 * 1.5).toInt()
			override val height: Int = (400 * 1.5).toInt()
			override val virtualWidth: Int = 550
			override val virtualHeight: Int = 400
		}, sceneClass = MyScene::class.java)
	}

	class MyScene : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			//sceneView += LocalVfs("c:/temp/test29/test29.swf").readSWFDeserializing(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/tt6.swf").readSWFDeserializing(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/tt5.swf").readSWFDeserializing(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/test3.swf").readSWFDeserializing(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/test3.swf").readSWF(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/sample1.swf").readSWF(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/tt11.swf").readSWFDeserializing(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/test27.swf").readSWFDeserializing(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/tt9.swf").readSWFDeserializing(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/tt20.swf").readSWFDeserializing(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/tt22.swf").readSWFDeserializing(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/test6.swf").readSWF(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/tt30.swf").readSWF(views).createMainTimeLine()
			//sceneView += LocalVfs("c:/temp/test3.swf").readSWF(views).createMainTimeLine()
			sceneView += LocalVfs("c:/temp/main.swf").readSWF(views).createMainTimeLine().apply {
				this["action"]?.colorTransform = ColorTransform.Add(-255, -255, -255, 0)
			}
		}


		companion object {
			suspend fun VfsFile.readSWFDeserializing(views: Views, config: SWFExportConfig? = null): AnLibrary {
				val ani = this.readSWF(views, config)
				val aniFile = this.withExtension("ani")
				//ani.writeTo(aniFile, ani.swfExportConfig.toAnLibrarySerializerConfig(compression = 1.0))
				ani.writeTo(aniFile, ani.swfExportConfig.toAnLibrarySerializerConfig(compression = 0.3))
				//ani.writeTo(aniFile, ani.swfExportConfig.toAnLibrarySerializerConfig(compression = 0.0))
				println("ANI size:" + aniFile.size())
				return aniFile.readAni(views)
			}
		}
	}

}
