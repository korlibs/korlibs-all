package com.soywiz.korau

import com.soywiz.korau.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import org.junit.*

class KorauTest {
	val formats = AudioFormats().registerStandard()

	@Test
	fun name(): Unit = suspendTest {
		val sound = ResourcesVfs["wav1.wav"].readAudioData(formats)
		//sleep(0)
		//sound.play()
	}
}