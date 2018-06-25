package com.soywiz.korau.format.atrac3plus

import jpcsp.Memory
import jpcsp.media.codec.atrac3plus.Atrac3plusDecoder
import jpcsp.media.codec.atrac3plus.util.Atrac3Util
import kotlin.test.*
import java.io.File

class TestAgainstJavaImpl {
	@Test
	@Ignore
	fun name() {
		val bytes = File("c:/temp/bgm01.at3").readBytes()
		val mem = Memory()
		mem.writeBytes(0, bytes)
		val startOutputAddr = bytes.size
		var outputAddr = startOutputAddr

		val info = Atrac3Util.AtracFileInfo()
		Atrac3Util.analyzeRiffFile(mem, 0, bytes.size, info)
		println(info)
		val decoder = Atrac3plusDecoder()
		val outputChannels = 1
		decoder.init(info.atracBytesPerFrame, info.atracChannels, outputChannels, 0)
		//var cpos = 0xa4
		var cpos = 0x674
		while (true) {
			val read = decoder.decode(mem, cpos, bytes.size - cpos, outputAddr)
			println(read)
			if (read <= 0) break
			//cpos += read
			cpos += info.atracBytesPerFrame
			//outputAddr += read * 10
			outputAddr += decoder.numberOfSamples * outputChannels * 2
		}

		File("c:/temp/bgm01.raw").writeBytes(mem.readBytes(startOutputAddr, outputAddr - startOutputAddr))
	}
}