package com.soywiz.korau.format

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.vfs.ResourcesVfs
import kotlin.test.assertEquals

class DecodeTest {
	val formats = AudioFormats().register(WAV)

	@kotlin.test.Test
	fun wav() = syncTest {
		val wavContents = ResourcesVfs["wav1.wav"].read()
		val wavData = formats.decode(wavContents.openAsync())!!

		assertEquals("AudioData(rate=44100, channels=1, samples=22050)", "$wavData")
		val wavContentsGen = formats.encodeToByteArray(wavData, "out.wav")

		assertEquals(wavContents.toList(), wavContentsGen.toList())
	}

	@kotlin.test.Test
	fun wav24() = syncTest {
		val wavContents = ResourcesVfs["wav24.wav"].read()
		val wavData = formats.decode(wavContents.openAsync())!!

		assertEquals("AudioData(rate=48000, channels=1, samples=4120)", "$wavData")
		val wavContentsGen = formats.encodeToByteArray(wavData, "out.wav")

		//LocalVfs("c:/temp/lol.wav").write(wavContentsGen)
		//Assert.assertArrayEquals(wavContents, wavContentsGen)
	}
}