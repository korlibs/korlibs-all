package com.soywiz.korau.format.atrac3plus

import com.soywiz.korau.format.atrac3plus.util.Atrac3PlusUtil
import com.soywiz.korau.format.util.IMemory
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.stream.MemorySyncStream
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.stream.readU8
import com.soywiz.korio.stream.toByteArray
import java.io.File

object ExampleDecoder {
	@JvmStatic fun main(args: Array<String>) {
		val fileName = "c:\\temp\\bgm01.at3"
		val s = File(fileName).readBytes().openSync()
		val info = Atrac3PlusUtil.AtracFileInfo()
		Atrac3PlusUtil.analyzeRiffFile(s, 0, s.length.toInt(), info)
		println(info)

		val out = MemorySyncStream()
		val decoder = Atrac3plusDecoder()
		val nchannels = 1
		decoder.init(info.atracBytesPerFrame, info.atracChannels, nchannels, 0)

		val mem = object : IMemory {
			override fun read8(addr: Int): Int {
				if (addr >= s.length) return -1
				s.position = addr.toLong()
				return s.readU8()
			}
		}

		// 744 * 2
		var ipos = 1652
		while (true) {
			val res = decoder.decode(mem, ipos, s.length.toInt(), out)
			println(res)
			if (res <= 0) break
			ipos += info.atracBytesPerFrame
		}
		File("c:/temp/temp.bin").writeBytes(out.toByteArray())
	}

}