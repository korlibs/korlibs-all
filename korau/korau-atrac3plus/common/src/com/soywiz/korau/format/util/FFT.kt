package com.soywiz.korau.format.util

import com.soywiz.klogger.Logger
import com.soywiz.kmem.arraycopy
import com.soywiz.korio.lang.format
import kotlin.math.*

class FFT {
	internal var nbits: Int = 0
	internal var inverse: Boolean = false
	internal var revtab: IntArray = IntArray(0)
	internal var tmpBuf: FloatArray = FloatArray(0)
	internal var mdctSize: Int = 0 // size of MDCT (i.e. number of input data * 2)
	internal var mdctBits: Int = 0 // n = 2^nbits
	// pre/post rotation tables
	internal var tcos = FloatArray(0)
	internal var tsin = FloatArray(0)

	fun copy(that: FFT) {
		nbits = that.nbits
		inverse = that.inverse
		copy(revtab, that.revtab)
		copy(tmpBuf, that.tmpBuf)
		mdctSize = that.mdctSize
		mdctBits = that.mdctBits
		copy(tcos, that.tcos)
		copy(tsin, that.tsin)
	}

	fun copy(dst: IntArray, src: IntArray) = arraycopy(src, 0, dst, 0, src.size)
	fun copy(dst: FloatArray, src: FloatArray) = arraycopy(src, 0, dst, 0, src.size)

	private fun fftInit(nbits: Int, inverse: Boolean): Int {
		if (nbits < 2 || nbits > 16) {
			revtab = IntArray(0)
			tmpBuf = FloatArray(0)
			return -1
		}

		this.nbits = nbits
		this.inverse = inverse

		val n = 1 shl nbits
		revtab = IntArray(n)
		tmpBuf = FloatArray(n * 2)

		initFfCosTabs(ff_cos_16, 16)
		initFfCosTabs(ff_cos_32, 32)
		initFfCosTabs(ff_cos_64, 64)
		initFfCosTabs(ff_cos_128, 128)
		initFfCosTabs(ff_cos_256, 256)
		initFfCosTabs(ff_cos_512, 512)

		for (i in 0 until n) {
			revtab[-splitRadixPermutation(i, n, inverse) and n - 1] = i
		}

		return 0
	}

	fun mdctInit(nbits: Int, inverse: Boolean, scale: Double): Int {
		var scale = scale
		val n = 1 shl nbits
		mdctBits = nbits
		mdctSize = n
		val n4 = n shr 2

		val ret = fftInit(mdctBits - 2, inverse)
		if (ret < 0) {
			return ret
		}

		tcos = FloatArray(n4)
		tsin = FloatArray(n4)

		val theta = 1.0 / 8.0 + if (scale < 0) n4 else 0
		scale = sqrt(abs(scale))
		for (i in 0 until n4) {
			val alpha = 2.0 * PI * (i + theta) / n
			tcos[i] = (-cos(alpha) * scale).toFloat()
			tsin[i] = (-sin(alpha) * scale).toFloat()
		}

		return 0
	}

	/**
	 * Compute inverse MDCT of size N = 2^nbits
	 * @param output N samples
	 * @param input N/2 samples
	 */
	fun imdctCalc(output: FloatArray, outputOffset: Int, input: FloatArray, inputOffset: Int) {
		val n = 1 shl mdctBits
		val n2 = n shr 1
		val n4 = n shr 2

		imdctHalf(output, outputOffset + n4, input, inputOffset)

		for (k in 0 until n4) {
			output[outputOffset + k] = -output[outputOffset + n2 - k - 1]
			output[outputOffset + n - k - 1] = output[outputOffset + n2 + k]
		}
	}

	/**
	 * Compute the middle half of the inverse MDCT of size N = 2^nbits,
	 * thus excluding the parts that can be derived by symmetry
	 * @param output N/2 samples
	 * @param input N/2 samples
	 */
	fun imdctHalf(output: FloatArray, outputOffset: Int, input: FloatArray, inputOffset: Int) {
		val n = 1 shl mdctBits
		val n2 = n shr 1
		val n4 = n shr 2
		val n8 = n shr 3

		// pre rotation
		var in1 = 0
		var in2 = n2 - 1
		for (k in 0 until n4) {
			val j = revtab!![k]
			CMUL(output, outputOffset + j * 2, outputOffset + j * 2 + 1, input[inputOffset + in2], input[inputOffset + in1], tcos[k], tsin[k])
			in1 += 2
			in2 -= 2
		}
		fftCalcFloat(output, outputOffset)

		// post rotation + reordering
		val r = FloatArray(4)
		for (k in 0 until n8) {
			CMUL(r, 0, 3, output[outputOffset + (n8 - k - 1) * 2 + 1], output[outputOffset + (n8 - k - 1) * 2 + 0], tsin[n8 - k - 1], tcos[n8 - k - 1])
			CMUL(r, 2, 1, output[outputOffset + (n8 + k) * 2 + 1], output[outputOffset + (n8 + k) * 2 + 0], tsin[n8 + k], tcos[n8 + k])
			output[outputOffset + (n8 - k - 1) * 2 + 0] = r[0]
			output[outputOffset + (n8 - k - 1) * 2 + 1] = r[1]
			output[outputOffset + (n8 + k) * 2 + 0] = r[2]
			output[outputOffset + (n8 + k) * 2 + 1] = r[3]
		}
	}

	private fun fft4(z: FloatArray, o: Int) {
		// BF(t3, t1, z[0].re, z[1].re);
		// BF(t8, t6, z[3].re, z[2].re);
		// BF(z[2].re, z[0].re, t1, t6);
		// BF(t4, t2, z[0].im, z[1].im);
		// BF(t7, t5, z[2].im, z[3].im);
		// BF(z[3].im, z[1].im, t4, t8);
		// BF(z[3].re, z[1].re, t3, t7);
		// BF(z[2].im, z[0].im, t2, t5);
		val t3 = (z[o + 0] - z[o + 2]).toDouble()
		val t1 = (z[o + 0] + z[o + 2]).toDouble()
		val t8 = (z[o + 6] - z[o + 4]).toDouble()
		val t6 = (z[o + 6] + z[o + 4]).toDouble()
		z[o + 4] = (t1 - t6).toFloat()
		z[o + 0] = (t1 + t6).toFloat()
		val t4 = (z[o + 1] - z[o + 3]).toDouble()
		val t2 = (z[o + 1] + z[o + 3]).toDouble()
		val t7 = (z[o + 5] - z[o + 7]).toDouble()
		val t5 = (z[o + 5] + z[o + 7]).toDouble()
		z[o + 7] = (t4 - t8).toFloat()
		z[o + 3] = (t4 + t8).toFloat()
		z[o + 6] = (t3 - t7).toFloat()
		z[o + 2] = (t3 + t7).toFloat()
		z[o + 5] = (t2 - t5).toFloat()
		z[o + 1] = (t2 + t5).toFloat()
	}

	private fun fft8(z: FloatArray, o: Int) {
		fft4(z, o)

		// BF(t1, z[5].re, z[4].re, -z[5].re);
		// BF(t2, z[5].im, z[4].im, -z[5].im);
		// BF(t5, z[7].re, z[6].re, -z[7].re);
		// BF(t6, z[7].im, z[6].im, -z[7].im);
		var t1 = (z[o + 8] + z[o + 10]).toDouble()
		z[o + 10] = z[o + 8] - z[o + 10]
		var t2 = (z[o + 9] + z[o + 11]).toDouble()
		z[o + 11] = z[o + 9] - z[o + 11]
		var t5 = (z[o + 12] + z[o + 14]).toDouble()
		z[o + 14] = z[o + 12] - z[o + 14]
		var t6 = (z[o + 13] + z[o + 15]).toDouble()
		z[o + 15] = z[o + 13] - z[o + 15]

		// BUTTERFLIES(z[0],z[2],z[4],z[6]);
		var t3 = t5 - t1
		t5 = t5 + t1
		z[o + 8] = (z[o + 0] - t5).toFloat()
		z[o + 0] = (z[o + 0] + t5).toFloat()
		z[o + 13] = (z[o + 5] - t3).toFloat()
		z[o + 5] = (z[o + 5] + t3).toFloat()
		var t4 = t2 - t6
		t6 = t2 + t6
		z[o + 12] = (z[o + 4] - t4).toFloat()
		z[o + 4] = (z[o + 4] + t4).toFloat()
		z[o + 9] = (z[o + 1] - t6).toFloat()
		z[o + 1] = (z[o + 1] + t6).toFloat()

		// TRANSFORM(z[1],z[3],z[5],z[7],sqrthalf,sqrthalf);
		//   CMUL(t1, t2, a2.re, a2.im, wre, -wim);
		t1 = (z[o + 10] * sqrthalf + z[o + 11] * sqrthalf).toDouble()
		t2 = (-z[o + 10] * sqrthalf + z[o + 11] * sqrthalf).toDouble()
		//   CMUL(t5, t6, a3.re, a3.im, wre,  wim);
		t5 = (z[o + 14] * sqrthalf - z[o + 15] * sqrthalf).toDouble()
		t6 = (z[o + 14] * sqrthalf + z[o + 15] * sqrthalf).toDouble()
		//   BUTTERFLIES(a0,a1,a2,a3)
		t3 = t5 - t1
		t5 = t5 + t1
		z[o + 10] = (z[o + 2] - t5).toFloat()
		z[o + 2] = (z[o + 2] + t5).toFloat()
		z[o + 15] = (z[o + 7] - t3).toFloat()
		z[o + 7] = (z[o + 7] + t3).toFloat()
		t4 = t2 - t6
		t6 = t2 + t6
		z[o + 14] = (z[o + 6] - t4).toFloat()
		z[o + 6] = (z[o + 6] + t4).toFloat()
		z[o + 11] = (z[o + 3] - t6).toFloat()
		z[o + 3] = (z[o + 3] + t6).toFloat()
	}

	private fun pass(z: FloatArray, o: Int, cos: FloatArray, n: Int) {
		var n = n
		var o0 = o
		var o1 = o + 2 * n * 2
		var o2 = o + 4 * n * 2
		var o3 = o + 6 * n * 2
		var wre = 0
		var wim = 2 * n
		n--

		// TRANSFORM_ZERO(z[0],z[o1],z[o2],z[o3]);
		var t1 = z[o2 + 0].toDouble()
		var t2 = z[o2 + 1].toDouble()
		var t5 = z[o3 + 0].toDouble()
		var t6 = z[o3 + 1].toDouble()
		//   BUTTERFLIES(a0,a1,a2,a3)
		var t3 = t5 - t1
		t5 = t5 + t1
		z[o2 + 0] = (z[o0 + 0] - t5).toFloat()
		z[o0 + 0] = (z[o0 + 0] + t5).toFloat()
		z[o3 + 1] = (z[o1 + 1] - t3).toFloat()
		z[o1 + 1] = (z[o1 + 1] + t3).toFloat()
		var t4 = t2 - t6
		t6 = t2 + t6
		z[o3 + 0] = (z[o1 + 0] - t4).toFloat()
		z[o1 + 0] = (z[o1 + 0] + t4).toFloat()
		z[o2 + 1] = (z[o0 + 1] - t6).toFloat()
		z[o0 + 1] = (z[o0 + 1] + t6).toFloat()
		// TRANSFORM(z[1],z[o1+1],z[o2+1],z[o3+1],wre[1],wim[-1]);
		//   CMUL(t1, t2, a2.re, a2.im, wre, -wim);
		t1 = (z[o2 + 2] * cos[wre + 1] + z[o2 + 3] * cos[wim - 1]).toDouble()
		t2 = (-z[o2 + 2] * cos[wim - 1] + z[o2 + 3] * cos[wre + 1]).toDouble()
		//   CMUL(t5, t6, a3.re, a3.im, wre,  wim);
		t5 = (z[o3 + 2] * cos[wre + 1] - z[o3 + 3] * cos[wim - 1]).toDouble()
		t6 = (z[o3 + 2] * cos[wim - 1] + z[o3 + 3] * cos[wre + 1]).toDouble()
		//   BUTTERFLIES(a0,a1,a2,a3)
		t3 = t5 - t1
		t5 = t5 + t1
		z[o2 + 2] = (z[o0 + 2] - t5).toFloat()
		z[o0 + 2] = (z[o0 + 2] + t5).toFloat()
		z[o3 + 3] = (z[o1 + 3] - t3).toFloat()
		z[o1 + 3] = (z[o1 + 3] + t3).toFloat()
		t4 = t2 - t6
		t6 = t2 + t6
		z[o3 + 2] = (z[o1 + 2] - t4).toFloat()
		z[o1 + 2] = (z[o1 + 2] + t4).toFloat()
		z[o2 + 3] = (z[o0 + 3] - t6).toFloat()
		z[o0 + 3] = (z[o0 + 3] + t6).toFloat()

		do {
			o0 += 4
			o1 += 4
			o2 += 4
			o3 += 4
			wre += 2
			wim -= 2
			// TRANSFORM(z[0],z[o1],z[o2],z[o3],wre[0],wim[0]);
			//   CMUL(t1, t2, a2.re, a2.im, wre, -wim);
			t1 = (z[o2 + 0] * cos[wre] + z[o2 + 1] * cos[wim]).toDouble()
			t2 = (-z[o2 + 0] * cos[wim] + z[o2 + 1] * cos[wre]).toDouble()
			//   CMUL(t5, t6, a3.re, a3.im, wre,  wim);
			t5 = (z[o3 + 0] * cos[wre] - z[o3 + 1] * cos[wim]).toDouble()
			t6 = (z[o3 + 0] * cos[wim] + z[o3 + 1] * cos[wre]).toDouble()
			//   BUTTERFLIES(a0,a1,a2,a3)
			t3 = t5 - t1
			t5 = t5 + t1
			z[o2 + 0] = (z[o0 + 0] - t5).toFloat()
			z[o0 + 0] = (z[o0 + 0] + t5).toFloat()
			z[o3 + 1] = (z[o1 + 1] - t3).toFloat()
			z[o1 + 1] = (z[o1 + 1] + t3).toFloat()
			t4 = t2 - t6
			t6 = t2 + t6
			z[o3 + 0] = (z[o1 + 0] - t4).toFloat()
			z[o1 + 0] = (z[o1 + 0] + t4).toFloat()
			z[o2 + 1] = (z[o0 + 1] - t6).toFloat()
			z[o0 + 1] = (z[o0 + 1] + t6).toFloat()
			// TRANSFORM(z[1],z[o1+1],z[o2+1],z[o3+1],wre[1],wim[-1]);
			//   CMUL(t1, t2, a2.re, a2.im, wre, -wim);
			t1 = (z[o2 + 2] * cos[wre + 1] + z[o2 + 3] * cos[wim - 1]).toDouble()
			t2 = (-z[o2 + 2] * cos[wim - 1] + z[o2 + 3] * cos[wre + 1]).toDouble()
			//   CMUL(t5, t6, a3.re, a3.im, wre,  wim);
			t5 = (z[o3 + 2] * cos[wre + 1] - z[o3 + 3] * cos[wim - 1]).toDouble()
			t6 = (z[o3 + 2] * cos[wim - 1] + z[o3 + 3] * cos[wre + 1]).toDouble()
			//   BUTTERFLIES(a0,a1,a2,a3)
			t3 = t5 - t1
			t5 = t5 + t1
			z[o2 + 2] = (z[o0 + 2] - t5).toFloat()
			z[o0 + 2] = (z[o0 + 2] + t5).toFloat()
			z[o3 + 3] = (z[o1 + 3] - t3).toFloat()
			z[o1 + 3] = (z[o1 + 3] + t3).toFloat()
			t4 = t2 - t6
			t6 = t2 + t6
			z[o3 + 2] = (z[o1 + 2] - t4).toFloat()
			z[o1 + 2] = (z[o1 + 2] + t4).toFloat()
			z[o2 + 3] = (z[o0 + 3] - t6).toFloat()
			z[o0 + 3] = (z[o0 + 3] + t6).toFloat()
		} while (--n != 0)
	}

	private fun fft16(z: FloatArray, o: Int) {
		fft8(z, o)
		fft4(z, o + 16)
		fft4(z, o + 24)
		pass(z, o, ff_cos_16, 2)
	}

	private fun fft32(z: FloatArray, o: Int) {
		fft16(z, o)
		fft8(z, o + 32)
		fft8(z, o + 48)
		pass(z, o, ff_cos_32, 4)
	}

	private fun fft64(z: FloatArray, o: Int) {
		fft32(z, o)
		fft16(z, o + 64)
		fft16(z, o + 96)
		pass(z, o, ff_cos_64, 8)
	}

	private fun fft128(z: FloatArray, o: Int) {
		fft64(z, o)
		fft32(z, o + 128)
		fft32(z, o + 192)
		pass(z, o, ff_cos_128, 16)
	}

	private fun fft256(z: FloatArray, o: Int) {
		fft128(z, o)
		fft64(z, o + 256)
		fft64(z, o + 384)
		pass(z, o, ff_cos_256, 32)
	}

	private fun fft512(z: FloatArray, o: Int) {
		fft256(z, o)
		fft128(z, o + 512)
		fft128(z, o + 768)
		pass(z, o, ff_cos_512, 64)
	}

	fun fftCalcFloat(z: FloatArray, o: Int) {
		when (nbits) {
			2 -> fft4(z, 0)
			3 -> fft8(z, o)
			4 -> fft16(z, 0)
			5 -> fft32(z, 0)
			6 -> fft64(z, o)
			7 -> fft128(z, o)
			8 -> fft256(z, 0)
			9 -> fft512(z, 0)
			else -> log.error { "FFT nbits=%d not implemented".format(nbits) }
		}
	}

	/**
	 * Compute MDCT of size N = 2^nbits
	 * @param input N samples
	 * @param out N/2 samples
	 */
	fun mdctCalc(output: FloatArray, outputOffset: Int, input: FloatArray, inputOffset: Int) {
		val n = 1 shl mdctBits
		val n2 = n shr 1
		val n4 = n shr 2
		val n8 = n shr 3
		val n3 = 3 * n4

		// pre rotation
		for (i in 0 until n8) {
			var re = -input[inputOffset + 2 * i + n3] - input[inputOffset + n3 - 1 - 2 * i]
			var im = -input[inputOffset + n4 + 2 * i] + input[inputOffset + n4 - 1 - 2 * i]
			var j = revtab!![i]
			CMUL(output, outputOffset + 2 * j + 0, outputOffset + 2 * j + 1, re, im, -tcos[i], tsin[i])

			re = input[inputOffset + 2 * i] - input[inputOffset + n2 - 1 - 2 * i]
			im = -input[inputOffset + n2 + 2 * i] - input[inputOffset + n - 1 - 2 * i]
			j = revtab!![n8 + i]
			CMUL(output, outputOffset + 2 * j + 0, outputOffset + 2 * j + 1, re, im, -tcos[n8 + i], tsin[n8 + i])
		}

		fftCalcFloat(output, outputOffset)

		// post rotation
		val r = FloatArray(4)
		for (i in 0 until n8) {
			CMUL(r, 3, 0, output[outputOffset + (n8 - i - 1) * 2 + 0], output[outputOffset + (n8 - i - 1) * 2 + 1], -tsin[n8 - i - 1], -tcos[n8 - i - 1])
			CMUL(r, 1, 2, output[outputOffset + (n8 + i) * 2 + 0], output[outputOffset + (n8 + i) * 2 + 1], -tsin[n8 + i], -tcos[n8 + i])
			output[outputOffset + (n8 - i - 1) * 2 + 0] = r[0]
			output[outputOffset + (n8 - i - 1) * 2 + 1] = r[1]
			output[outputOffset + (n8 + i) * 2 + 0] = r[2]
			output[outputOffset + (n8 + i) * 2 + 1] = r[3]
		}
	}

	companion object {
		private val log = Logger("FFT")
		val M_SQRT1_2 = 0.70710678118654752440 // 1/sqrt(2)
		private val sqrthalf = M_SQRT1_2.toFloat()
		private val ff_cos_16 = FloatArray(16 / 2)
		private val ff_cos_32 = FloatArray(32 / 2)
		private val ff_cos_64 = FloatArray(64 / 2)
		private val ff_cos_128 = FloatArray(128 / 2)
		private val ff_cos_256 = FloatArray(256 / 2)
		private val ff_cos_512 = FloatArray(512 / 2)

		private fun initFfCosTabs(tab: FloatArray, m: Int) {
			val freq = 2 * PI / m
			for (i in 0..m / 4) {
				tab[i] = cos(i * freq).toFloat()
			}
			for (i in 1 until m / 4) {
				tab[m / 2 - i] = tab[i]
			}
		}

		private fun splitRadixPermutation(i: Int, n: Int, inverse: Boolean): Int {
			if (n <= 2) {
				return i and 1
			}
			var m = n shr 1
			if (i and m == 0) {
				return splitRadixPermutation(i, m, inverse) * 2
			}
			m = m shr 1
			return splitRadixPermutation(i, m, inverse) * 4 + if (inverse == (i and m == 0)) 1 else -1
		}

		private fun CMUL(d: FloatArray, dre: Int, dim: Int, are: Float, aim: Float, bre: Float, bim: Float) {
			d[dre] = are * bre - aim * bim
			d[dim] = are * bim + aim * bre
		}
	}
}
