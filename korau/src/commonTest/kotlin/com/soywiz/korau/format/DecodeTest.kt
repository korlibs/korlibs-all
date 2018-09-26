package com.soywiz.korau.format

import com.soywiz.korio.async.*
import com.soywiz.korio.crypto.CRC32
import com.soywiz.korio.crypto.compute
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
import kotlin.test.*

class DecodeTest {
	val formats = AudioFormats().register(WAV)

	@kotlin.test.Test
	fun wav() = suspendTest {
		val wavContents = ResourcesVfs["wav1.wav"].read()
		assertEquals(44144, wavContents.size, "wavContents.size")
		assertEquals(0x901751CE.toInt(), CRC32.compute(wavContents), "wavContents.crc32")

		val wavData = formats.decode(wavContents.openAsync())!!

		assertEquals("AudioData(rate=44100, channels=1, samples=22050)", "$wavData")
		val wavContentsGen = formats.encodeToByteArray(wavData, "out.wav")

		assertEquals(wavContents.toList(), wavContentsGen.toList())
	}

	@kotlin.test.Test
	fun wav24() = suspendTest {
		val wavContents = ResourcesVfs["wav24.wav"].read()
		val wavData = formats.decode(wavContents.openAsync())!!

		assertEquals("AudioData(rate=48000, channels=1, samples=4120)", "$wavData")
		val wavContentsGen = formats.encodeToByteArray(wavData, "out.wav")

		//LocalVfs("c:/temp/lol.wav").write(wavContentsGen)
		//Assert.assertArrayEquals(wavContents, wavContentsGen)
	}
}