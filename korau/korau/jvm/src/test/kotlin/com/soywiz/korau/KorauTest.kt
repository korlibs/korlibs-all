package com.soywiz.korau

import com.soywiz.korau.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.vfs.*
import org.junit.*

class KorauTest {
	val formats = AudioFormats().registerStandard()

	@Test
	fun name(): Unit = syncTest {
		val sound = ResourcesVfs["wav1.wav"].readAudioData(formats)
		//sleep(0)
		//sound.play()
	}
}