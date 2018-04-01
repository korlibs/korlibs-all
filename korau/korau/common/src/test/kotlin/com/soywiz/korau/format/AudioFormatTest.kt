package com.soywiz.korau.format

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import kotlin.test.assertEquals

class AudioFormatTest {
	val formats = AudioFormats().registerStandard()

	@kotlin.test.Test
	fun wav() = syncTest {
		assertEquals(
			"Info(lengthInMicroseconds=500000, channels=1)",
			ResourcesVfs["wav1.wav"].readSoundInfo(formats).toString()
		)
		assertEquals(
			"Info(lengthInMicroseconds=500000, channels=1)",
			ResourcesVfs["wav2.wav"].readSoundInfo(formats).toString()
		)
	}

	@kotlin.test.Test
	fun ogg() = syncTest {
		assertEquals(
			"Info(lengthInMicroseconds=500000, channels=1)",
			ResourcesVfs["ogg1.ogg"].readSoundInfo(formats).toString()
		)
	}

	@kotlin.test.Test
	fun mp3() = syncTest {
		assertEquals(
			"Info(lengthInMicroseconds=546625, channels=1)",
			ResourcesVfs["mp31.mp3"].readSoundInfo(formats).toString()
		)
	}
}