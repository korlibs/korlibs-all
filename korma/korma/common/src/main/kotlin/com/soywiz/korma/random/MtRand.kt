package com.soywiz.korma.random

import com.soywiz.korma.math.Math

class MtRand private constructor(dummy: Boolean) : Rand {
	companion object {
		const private val N: Int = 624
		const private val M: Int = 397

		private fun twiddle(u: Int, v: Int): Int {
			return (((u and 0x80000000.toInt()) or (v and 0x7FFFFFFF)) ushr 1) xor (if ((v and 1) != 0) 0x9908B0DF.toInt() else 0x0)
		}
	}

	private var p: Int = N

	private var state = IntArray(N)

	constructor() : this(BaseRand.random().toBits())

	constructor(s: Int) : this(false) {
		seed(s)
	}

	constructor(s: Long) : this(false) {
		seed(s.toInt()) // Discard long part!
	}

	override fun seed(s: Int): MtRand {
		state[0] = s // for > 32 bit machines
		for (i in 1 until N) {
			state[i] = Math.multiplyIntegerUnsigned(1812433253, (state[i - 1] xor (state[i - 1] ushr 30))) + i
		}
		p = N // force genState() to be called for next random number
		return this
	}

	private fun genState() {
		for (i in 0 until N - M) {
			state[i] = state[i + M] xor twiddle(state[i], state[i + 1])
		}
		for (i in N - M until (N - 1)) {
			state[i] = state[i + M - N] xor twiddle(state[i], state[i + 1])
		}
		state[N - 1] = state[M - 1] xor twiddle(state[N - 1], state[0])
		p = 0 // reset position
	}

	override fun nextInt(): Int {
		if (p == N) genState()
		var x = state[p++]
		x = x xor (x ushr 11)
		x = x xor (x shl 7) and 0x9D2C5680.toInt()
		x = x xor (x shl 15) and 0xEFC60000.toInt()
		return x xor (x ushr 18)
	}

	override val maxValue: Int = 0xffffffff.toInt()
}