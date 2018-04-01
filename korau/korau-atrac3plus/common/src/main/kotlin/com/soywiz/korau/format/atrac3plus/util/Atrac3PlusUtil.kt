package com.soywiz.korau.format.atrac3plus.util

import com.soywiz.klogger.Logger
import com.soywiz.kmem.extract8
import com.soywiz.korio.lang.format
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readS32_le
import com.soywiz.korio.stream.readU16_le
import com.soywiz.korio.stream.readU8

/**
 * From JPCSP
 */
object Atrac3PlusUtil {
	val log = Logger("Atrac3PlusUtil")

	val AT3_MAGIC = 0x0270 // "AT3"
	val AT3_PLUS_MAGIC = 0xFFFE // "AT3PLUS"
	val RIFF_MAGIC = 0x46464952 // "RIFF"
	val WAVE_MAGIC = 0x45564157 // "WAVE"
	val FMT_CHUNK_MAGIC = 0x20746D66 // "FMT "
	val FACT_CHUNK_MAGIC = 0x74636166 // "FACT"
	val SMPL_CHUNK_MAGIC = 0x6C706D73 // "SMPL"
	val DATA_CHUNK_MAGIC = 0x61746164 // "DATA"

	val ATRAC3_CONTEXT_READ_SIZE_OFFSET = 160
	val ATRAC3_CONTEXT_REQUIRED_SIZE_OFFSET = 164
	val ATRAC3_CONTEXT_DECODE_RESULT_OFFSET = 188

	val PSP_ATRAC_ALLDATA_IS_ON_MEMORY = -1
	val PSP_ATRAC_NONLOOP_STREAM_DATA_IS_ON_MEMORY = -2
	val PSP_ATRAC_LOOP_STREAM_DATA_IS_ON_MEMORY = -3

	val PSP_ATRAC_STATUS_NONLOOP_STREAM_DATA = 0
	val PSP_ATRAC_STATUS_LOOP_STREAM_DATA = 1

	val ATRAC_HEADER_HASH_LENGTH = 512
	val ERROR_ATRAC_UNKNOWN_FORMAT = -0x7f9cfffa
	val ERROR_ATRAC_INVALID_SIZE = -0x7f9cffef

	val PSP_CODEC_AT3PLUS = 0x00001000
	val PSP_CODEC_AT3 = 0x00001001
	val PSP_CODEC_MP3 = 0x00001002
	val PSP_CODEC_AAC = 0x00001003

	private fun readUnaligned32(mem: SyncStream, addr: Int): Int {
		mem.position = addr.toLong()
		return mem.readS32_le()
	}

	fun SyncStream.read8(addr: Int): Int {
		this.position = addr.toLong()
		return this.readU8()
	}

	fun SyncStream.read16(addr: Int): Int {
		this.position = addr.toLong()
		return this.readU16_le()
	}

	/**
	 * From JPCSP
	 */
	fun analyzeRiffFile(mem: SyncStream, addr: Int, length: Int, info: AtracFileInfo): Int {
		var result = ERROR_ATRAC_UNKNOWN_FORMAT

		var currentAddr = addr
		var bufferSize = length
		info.atracEndSample = -1
		info.numLoops = 0
		info.inputFileDataOffset = 0

		if (bufferSize < 12) {
			log.error("Atrac buffer too small %d".format(bufferSize))
			return ERROR_ATRAC_INVALID_SIZE
		}

		// RIFF file format:
		// Offset 0: 'RIFF'
		// Offset 4: file length - 8
		// Offset 8: 'WAVE'
		val magic = readUnaligned32(mem, currentAddr)
		val WAVEMagic = readUnaligned32(mem, currentAddr + 8)
		if (magic != RIFF_MAGIC || WAVEMagic != WAVE_MAGIC) {
			//log.error(String_format("Not a RIFF/WAVE format! %s", Utilities.getMemoryDump(currentAddr, 16)))
            log.error { "Not a RIFF/WAVE format!" }
			return ERROR_ATRAC_UNKNOWN_FORMAT
		}

		info.inputFileSize = readUnaligned32(mem, currentAddr + 4) + 8
		info.inputDataSize = info.inputFileSize
		//if (log.isDebugEnabled()) {
		log.trace { "FileSize 0x%X".format(info.inputFileSize) }
		//}
		currentAddr += 12
		bufferSize -= 12

		var foundData = false
		while (bufferSize >= 8 && !foundData) {
			val chunkMagic = readUnaligned32(mem, currentAddr)
			val chunkSize = readUnaligned32(mem, currentAddr + 4)
			currentAddr += 8
			bufferSize -= 8

			when (chunkMagic) {
				DATA_CHUNK_MAGIC -> {
					foundData = true
					// Offset of the data chunk in the input file
					info.inputFileDataOffset = currentAddr - addr
					info.inputDataSize = chunkSize
					log.trace { "DATA Chunk: data offset=0x%X, data size=0x%X".format(info.inputFileDataOffset, info.inputDataSize) }
				}
				FMT_CHUNK_MAGIC -> {
					if (chunkSize >= 16) {
						val compressionCode = mem.read16(currentAddr)
						info.atracChannels = mem.read16(currentAddr + 2)
						info.atracSampleRate = readUnaligned32(mem, currentAddr + 4)
						info.atracBitrate = readUnaligned32(mem, currentAddr + 8)
						info.atracBytesPerFrame = mem.read16(currentAddr + 12)
						val hiBytesPerSample = mem.read16(currentAddr + 14)
						val extraDataSize = mem.read16(currentAddr + 16)
						if (extraDataSize == 14) {
							info.atracCodingMode = mem.read16(currentAddr + 18 + 6)
						}
						if (log.isTraceEnabled) {
							log.trace { "WAVE format: magic=0x%08X('%s'), chunkSize=%d, compressionCode=0x%04X, channels=%d, sampleRate=%d, bitrate=%d, bytesPerFrame=0x%X, hiBytesPerSample=%d, codingMode=%d".format(chunkMagic, getStringFromInt32(chunkMagic), chunkSize, compressionCode, info.atracChannels, info.atracSampleRate, info.atracBitrate, info.atracBytesPerFrame, hiBytesPerSample, info.atracCodingMode) }
							// Display rest of chunk as debug information
							val restChunk = StringBuilder()
							for (i in 16 until chunkSize) {
								val b = mem.read8(currentAddr + i)
								restChunk.append(" %02X".format(b))
							}
							if (restChunk.length > 0) {
								log.trace { "Additional chunk data:%s".format(restChunk) }
							}
						}

						if (compressionCode == AT3_MAGIC) {
							result = PSP_CODEC_AT3
						} else if (compressionCode == AT3_PLUS_MAGIC) {
							result = PSP_CODEC_AT3PLUS
						} else {
							return ERROR_ATRAC_UNKNOWN_FORMAT
						}
					}
				}
				FACT_CHUNK_MAGIC -> {
					if (chunkSize >= 8) {
						info.atracEndSample = readUnaligned32(mem, currentAddr)
						if (info.atracEndSample > 0) {
							info.atracEndSample -= 1
						}
						if (chunkSize >= 12) {
							// Is the value at offset 4 ignored?
							info.atracSampleOffset = readUnaligned32(mem, currentAddr + 8) // The loop samples are offset by this value
						} else {
							info.atracSampleOffset = readUnaligned32(mem, currentAddr + 4) // The loop samples are offset by this value
						}
						log.trace { "FACT Chunk: chunkSize=%d, endSample=0x%X, sampleOffset=0x%X".format(chunkSize, info.atracEndSample, info.atracSampleOffset) }
					}
				}
				SMPL_CHUNK_MAGIC -> {
					if (chunkSize >= 36) {
						val checkNumLoops = readUnaligned32(mem, currentAddr + 28)
						if (chunkSize >= 36 + checkNumLoops * 24) {
							info.numLoops = checkNumLoops
							info.loops = Array<LoopInfo>(info.numLoops) { LoopInfo() }
							var loopInfoAddr = currentAddr + 36
							for (i in 0 until info.numLoops) {
								val loop = info.loops[i]
								info.loops[i] = loop
								loop.cuePointID = readUnaligned32(mem, loopInfoAddr)
								loop.type = readUnaligned32(mem, loopInfoAddr + 4)
								loop.startSample = readUnaligned32(mem, loopInfoAddr + 8) - info.atracSampleOffset
								loop.endSample = readUnaligned32(mem, loopInfoAddr + 12) - info.atracSampleOffset
								loop.fraction = readUnaligned32(mem, loopInfoAddr + 16)
								loop.playCount = readUnaligned32(mem, loopInfoAddr + 20)

								log.trace { "Loop #%d: %s".format(i, loop.toString()) }
								loopInfoAddr += 24
							}
							// TODO Second buffer processing disabled because still incomplete
							//isSecondBufferNeeded = true;
						}
					}
				}
			}

			if (chunkSize > bufferSize) {
				break
			}

			currentAddr += chunkSize
			bufferSize -= chunkSize
		}

		// If a loop end is past the atrac end, assume the atrac end
		for (loop in info.loops) {
			if (loop.endSample > info.atracEndSample) {
				loop.endSample = info.atracEndSample
			}
		}

		return result
	}

	private fun getStringFromInt32(chunkMagic: Int): String = charArrayOf(chunkMagic.extract8(0).toChar(), chunkMagic.extract8(8).toChar(), chunkMagic.extract8(16).toChar(), chunkMagic.extract8(24).toChar()).contentToString()

	class LoopInfo {
		var cuePointID: Int = 0
		var type: Int = 0
		var startSample: Int = 0
		var endSample: Int = 0
		var fraction: Int = 0
		var playCount: Int = 0

		override fun toString(): String =
			"LoopInfo[cuePointID %d, type %d, startSample 0x%X, endSample 0x%X, fraction %d, playCount %d]".format(cuePointID, type, startSample, endSample, fraction, playCount)
	}

	data class AtracFileInfo(
		var atracBitrate: Int = 64,
		var atracChannels: Int = 2,
		var atracSampleRate: Int = 0xAC44,
		var atracBytesPerFrame: Int = 0x0230,
		var atracEndSample: Int = 0,
		var atracSampleOffset: Int = 0,
		var atracCodingMode: Int = 0,
		var inputFileDataOffset: Int = 0,
		var inputFileSize: Int = 0,
		var inputDataSize: Int = 0,
		var loopNum: Int = 0,
		var numLoops: Int = 0,
		var loops: Array<LoopInfo> = arrayOf()
	)
}