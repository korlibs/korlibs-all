package com.soywiz.korau

import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.readAudioData
import com.soywiz.korau.format.registerStandard
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test

class KorauTest {
	val formats = AudioFormats().registerStandard()

	@Test
	fun name(): Unit = syncTest {
		val sound = ResourcesVfs["wav1.wav"].readAudioData(formats)
		//sleep(0)
		//sound.play()
	}
}