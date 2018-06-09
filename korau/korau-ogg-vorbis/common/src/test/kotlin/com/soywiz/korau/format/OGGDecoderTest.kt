package com.soywiz.korau.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class OGGDecoderTest {
	val formats = AudioFormats().registerStandard().registerOggVorbisDecoder()

	@Test
	fun testDecodeWav() = suspendTest {
		//ResourcesVfs["ogg1.ogg"].readAudioData().play()
		//ResourcesVfs["ogg1.ogg"].readAudioStream()!!.play()
		val expected = ResourcesVfs["ogg1.ogg.wav"].readAudioData(formats)
		val output = ResourcesVfs["ogg1.ogg"].readAudioData(formats)

		val expectedBytes = formats.encodeToByteArray(expected, "out.wav")
		val outputBytes = formats.encodeToByteArray(output, "out.wav")

		//output.play()
		//expected.play()
		assertEquals(expectedBytes.toList(), outputBytes.toList())

		//LocalVfs("c:/temp/test.ogg").readAudioStream()!!.play()
	}
}