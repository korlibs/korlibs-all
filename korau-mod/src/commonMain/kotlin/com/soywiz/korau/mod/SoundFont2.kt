package com.soywiz.korau.mod

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.file.*

suspend fun VfsFile.readSoundFont2(): SoundFont = SoundFont2Reader().apply { read(readAsSyncStream()) }

interface SoundFont2 : SoundFont {
	val presets: List<Preset>

	data class Preset(
		val presetName: String,
		val preset: Int,
		val bank: Int,
		val presetBagIdx: Int,
		val library: Int,
		val genre: Int,
		val morphology: Int
	)

	data class Sample(
		val name: String,
		val start: Int,
		val end: Int,
		val startLoop: Int,
		val endLoop: Int,
		val sampleRate: Int,
		val originalKey: Int,
		val correction: Int,
		val sampleLink: Int,
		val sampleType: Int
	)

	data class Inst(val name: String, val bagIdx: Int)
	data class Bag(val genIdx: Int, val modIdx: Int)
	data class Gen(val oper: Int, val amount: Int)
	data class Mod(
		val srcOp: Int,
		val dst: Int,
		val modAmount: Int,
		val src2Op: Int,
		val transform: Int
	)

	data class Generator(
		var instrument: Int = 0,
		var sampleID: Int = 0
	) {
		var mod: Mod = Mod(0, 0, 0, 0, 0)
		var startAddrsOffset: Int = 0
		var endAddrsOffset: Int = 0
		var startloopAddrsOffset: Int = 0
		var endloopAddrsOffset: Int = 0
		var startAddrsCoarseOffset: Int = 0
		var modLfoToPitch: Int = 0
		var vibLfoToPitch: Int = 0
		var modEnvToPitch: Int = 0
		var initialFilterFc: Int = 13500
		var initialFilterQ: Int = 0
		var modLfoToFilterFc: Int = 0
		var modEnvToFilterFc: Int = 0
		var endAddrsCoarseOffset: Int = 0
		var modLfoToVolume: Int = 0
		var chorusEffectsSend: Int = 0
		var reverbEffectsSend: Int = 0
		var pan: Int = 0
		var delayModLFO: Int = -12000
		var freqModLFO: Int = 0
		var delayVibLFO: Int = -12000
		var freqVibLFO: Int = 0
		var delayModEnv: Int = -12000
		var attackModEnv: Int = -12000
		var holdModEnv: Int = -12000
		var decayModEnv: Int = -12000
		var sustainModEnv: Int = 0
		var releaseModEnv: Int = -12000
		var keynumToModEnvHold: Int = 0
		var keynumToModEnvDecay: Int = 0
		var delayVolEnv: Int = -12000
		var attackVolEnv: Int = -12000
		var holdVolEnv: Int = -12000
		var decayVolEnv: Int = -12000
		var sustainVolEnv: Int = 0
		var releaseVolEnv: Int = -12000
		var keynumToVolEnvHold: Int = 0
		var keynumToVolEnvDecay: Int = 0
		var keyRange: IntArray = intArrayOf(0, 127)
		var velRange: IntArray = intArrayOf(0, 127)
		var startloopAddrsCoarseOffset: Int = 0
		var keynum: Int = -1
		var velocity: Int = -1
		var initialAttenuation: Int = 0
		var endloopAddrsCoarseOffset: Int = 0
		var coarseTune: Int = 0
		var fineTune: Int = 0
		var sampleMode: Int = 0
		var scaleTuning: Int = 100
		var exclusiveClass: Int = 0
		var overridingRootKey: Int = -1

		fun apply(gen: SoundFont2.Gen) {
			val oper = gen.oper
			val value = gen.amount
			when (oper) {
				0 -> run { startAddrsOffset = value }
				1 -> run { endAddrsOffset = value }
				2 -> run { startloopAddrsOffset = value }
				3 -> run { endloopAddrsOffset = value }
				4 -> run { startAddrsCoarseOffset = value }
				5 -> run { modLfoToPitch = value }
				6 -> run { vibLfoToPitch = value }
				7 -> run { modEnvToPitch = value }
				8 -> run { initialFilterFc = value }
				9 -> run { initialFilterQ = value }
				10 -> run { modLfoToFilterFc = value }
				11 -> run { modEnvToFilterFc = value }
				12 -> run { endAddrsCoarseOffset = value }
				13 -> run { modLfoToVolume = value }
				14 -> run { } // unused1
				15 -> run { chorusEffectsSend = value }
				16 -> run { reverbEffectsSend = value }
				17 -> run { pan = value }
				18 -> run { } // unused2
				19 -> run { } // unused3
				20 -> run { } // unused4
				21 -> run { delayModLFO = value }
				22 -> run { freqModLFO = value }
				23 -> run { delayVibLFO = value }
				24 -> run { freqVibLFO = value }
				25 -> run { delayModEnv = value }
				26 -> run { attackModEnv = value }
				27 -> run { holdModEnv = value }
				28 -> run { decayModEnv = value }
				29 -> run { sustainModEnv = value }
				30 -> run { releaseModEnv = value }
				31 -> run { keynumToModEnvHold = value }
				32 -> run { keynumToModEnvDecay = value }
				33 -> run { delayVolEnv = value }
				34 -> run { attackVolEnv = value }
				35 -> run { holdVolEnv = value }
				36 -> run { decayVolEnv = value }
				37 -> run { sustainVolEnv = value }
				38 -> run { releaseVolEnv = value }
				39 -> run { keynumToVolEnvHold = value }
				40 -> run { keynumToVolEnvDecay = value }
				41 -> run { instrument = value }
				42 -> run { } // reserved1
				43 -> run { keyRange = intArrayOf(value.extract8Signed(0), value.extract8Signed(8)) }
				44 -> run { velRange = intArrayOf(value.extract8Signed(0), value.extract8Signed(8)) }
				45 -> run { startloopAddrsCoarseOffset = value }
				46 -> run { keynum = value }
				47 -> run { velocity = value }
				48 -> run { initialAttenuation = value }
				49 -> run { } // reserved2
				50 -> run { endloopAddrsCoarseOffset = value }
				51 -> run { coarseTune = value }
				52 -> run { fineTune = value }
				53 -> run { sampleID = value }
				54 -> run { sampleMode = value }
				55 -> run { } // reserved3
				56 -> run { scaleTuning = value }
				57 -> run { exclusiveClass = value }
				58 -> run { overridingRootKey = value }
				59 -> run { } // unused5
				60 -> run { } // endOper
				else -> invalidOp("OPER: $oper")
			}
		}
	}

	data class Instrument(val name: String, val gens: List<Generator>)
}

private class SoundFont2Reader : SoundFont2 {
	var INAM = ""
	var versionMajor = 0
	var versionMinor = 0
	var sampleData = shortArrayOf()
	var samples = listOf<SoundFont2.Sample>()
	override var presets = listOf<SoundFont2.Preset>()

	class Area {
		var bag = listOf<SoundFont2.Bag>()
		var gen = listOf<SoundFont2.Gen>()
		var mod = listOf<SoundFont2.Mod>()
		val generators: List<SoundFont2.Generator> by lazy {
			(0 until bag.size - 1).map {
				val g = SoundFont2.Generator()
				g.mod = mod[bag[it].modIdx]
				val gen1 = bag[it].genIdx
				val gen2 = bag[it + 1].genIdx
				for (i in gen1 until gen2) {
					g.apply(gen[i])
				}
				g
			}
		}
	}

	val parea = Area()
	val iarea = Area()
	var insts = listOf<SoundFont2.Inst>()
	val instruments by lazy {
		(0 until insts.size - 1).map {
			val i0 = insts[it]
			val i1 = insts[it + 1]
			val idx0 = i0.bagIdx
			val idx1 = i1.bagIdx
			val zones = (idx0 until idx1).map { iarea.generators[it] }
			SoundFont2.Instrument(i0.name, zones)
		}
	}

	data class ProcessedPreset(
		val preset: SoundFont2.Preset,
		val generator: SoundFont2.Generator,
		val instrument: SoundFont2.Instrument
	)

	val ppresets by lazy {
		presets.map {
			val pgen = parea.generators.getOrNull(it.presetBagIdx)
			val i = pgen?.instrument?.let { instruments.getOrNull(it) }
			if (pgen != null && i != null) ProcessedPreset(it, pgen, i) else null
		}.filterNotNull()
	}

	val ppresetsById by lazy {
		ppresets.associateBy { it.preset.preset }
	}

	fun ProcessedPreset.createPatch(): SoundPatch {
		return object : SoundPatch {
			val preset = this@createPatch
			val sampleIDS = preset.instrument.gens.map { it.sampleID }
			val samples = sampleIDS.map { this@SoundFont2Reader.samples[it] }
			override fun getSample(key: Int, time: Int): Float {
				return 0f
			}
		}
	}

	val patchesById: Map<Int, SoundPatch> by lazy {
		ppresets.map { it.preset.preset to it.createPatch() }.toMap()
	}

	override fun get(patch: Int): SoundPatch = patchesById[patch] ?: invalidOp("Can't find patch $patch")

	fun read(s: SyncStream) {
		if (s.sliceWithSize(0, 4).readStringz(4) != "RIFF") invalidOp("Not a SF2 file")
		if (s.sliceWithSize(8, 4).readStringz(4) != "sfbk") invalidOp("Not a SF2 file")
		s.readSF2Layer(0)
	}

	fun SyncStream.readSF2Layer(kind: Int) {
		while (!eof) {
			val id = readStringz(4)
			val size = readS32_le()
			val type = if (kind == 0) readStringz(4) else ""
			val s = if (kind == 0) readStream(size - 4) else readStream(size)
			val rname = "$id$type"

			s.run {
				when (rname) {
					"LISTINFO" -> {
						s.readSF2Layer(1)
					}
					"RIFFsfbk" -> {
						readSF2Layer(0)
					}
					"ifil" -> { // http://www.pjb.com.au/midi/sfspec21.html#5.1
						versionMajor = readS16_le()
						versionMinor = readS16_le()
					}
					"INAM" -> {// http://www.pjb.com.au/midi/sfspec21.html#5.3
						INAM = readStringz(UTF8)
					}
					"LISTsdta" -> { // http://www.pjb.com.au/midi/sfspec21.html#6
						sampleData = readShortArray_le(s.available.toInt() / 2)
					}
					"LISTpdta" -> { // http://www.pjb.com.au/midi/sfspec21.html#7
						readSF2Layer(1)
					}
					"phdr" -> { // Preset Header :: http://www.pjb.com.au/midi/sfspec21.html#7.2
						presets = mapWhile({ !eof }) {
							SoundFont2.Preset(
								presetName = readStringz(20, UTF8),
								preset = readS16_le(),
								bank = readS16_le(),
								presetBagIdx = readS16_le(),
								library = readS32_le(),
								genre = readS32_le(),
								morphology = readS32_le()
							)
						}
					}
					"pbag", "ibag" -> { // http://www.pjb.com.au/midi/sfspec21.html#7.3
						val area = if (rname == "pbag") parea else iarea
						area.bag = mapWhile({ !eof }) {
							SoundFont2.Bag(
								genIdx = readS16_le(),
								modIdx = readS16_le()
							)
						}
					}
					"pmod", "imod" -> {
						val area = if (rname == "pmod") parea else iarea
						area.mod = mapWhile({ !eof }) {
							SoundFont2.Mod(
								srcOp = readU16_le(),
								dst = readU16_le(),
								modAmount = readU16_le(),
								src2Op = readU16_le(),
								transform = readU16_le()
							)
						}
					}
					"pgen", "igen" -> {
						val area = if (rname == "pgen") parea else iarea
						area.gen = mapWhile({ !eof }) {
							SoundFont2.Gen(
								oper = readS16_le(),
								amount = readS16_le()
							)
						}
					}
					"inst" -> {
						insts = mapWhile({ !eof }) {
							SoundFont2.Inst(
								name = readStringz(20, UTF8),
								bagIdx = readU16_le()
							)
						}
					}
					"shdr" -> { // http://www.pjb.com.au/midi/sfspec21.html#7.10
						samples = mapWhile({ !eof }) {
							SoundFont2.Sample(
								name = readStringz(20, UTF8),
								start = readS32_le(),
								end = readS32_le(),
								startLoop = readS32_le(),
								endLoop = readS32_le(),
								sampleRate = readS32_le(),
								originalKey = readS8(),
								correction = readU8(),
								sampleLink = readU16_le(),
								sampleType = readU16_le()
							)
						}
					}
					else -> {
						error("Unknown '$id$type'")
					}
				}
			}
		}
	}

}

