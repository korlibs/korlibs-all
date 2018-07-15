package com.soywiz.korma.random

import com.soywiz.korma.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*

interface Rand {
	val maxValue: Int
	fun seed(s: Int): Rand
	fun nextInt(): Int

	companion object {
		operator fun invoke(): Rand = MtRand()
	    operator fun invoke(seed: Int): Rand = MtRand(seed)
		operator fun invoke(seed: Long): Rand = MtRand(seed)
	}
}

fun Rand.nextInt(max: Int): Int {
	return (nextInt() and 0x7FFFFFFF) % max
}

fun Rand.nextLong(): Long {
	val low = nextInt()
	val high = nextInt()
	return (high.toLong() shl 32) or (low.toLong() and 0xFFFFFFFFL)
}

fun Rand.nextDouble(): Double {
	val v = (nextInt() and 0x7FFFFFFF) // Unsigned!
	return v.toDouble() / 0x7FFFFFFF.toDouble()
}

fun <T> List<T>.getCyclic(index: Int) = this[index % this.size]

fun <T> List<T>.random(random: Rand = Rand()): T {
	if (this.isEmpty()) throw IllegalArgumentException("Empty list")
	return this[random.nextInt(this.size)]
}

operator fun Rand.get(min: Double, max: Double): Double = min + nextDouble() * (max - min)
operator fun Rand.get(min: Int, max: Int): Int = min + nextInt(max - min)
operator fun Rand.get(range: IntRange): Int = range.start + this.nextInt(range.endInclusive - range.start + 1)
operator fun Rand.get(range: LongRange): Long = range.start + this.nextLong() % (range.endInclusive - range.start + 1)

operator fun <T> Rand.get(list: List<T>): T = list[this[list.indices]]

operator fun Rand.get(rectangle: Rectangle): Point2d =
	Vector2(this[rectangle.left, rectangle.right], this[rectangle.top, rectangle.bottom])

operator fun <T : Interpolable<T>> Rand.get(l: T, r: T): T =
	(this.nextInt(0x10001).toDouble() / 0x10000.toDouble()).interpolate(l, r)

operator fun <T : Comparable<T>> Rand.get(range: ClosedRange<T>): T {
	return interpolateAny(range.start, range.endInclusive, (this.nextInt(0x10001).toDouble() / 0x10000.toDouble()))
}

operator fun <T> Rand.get(weighted: Map<T, Int>): T {
	val totalWeight = weighted.values.sum()
	val found = this[0, totalWeight + 1]

	var offset = 0
	for ((key, len) in weighted.entries) {
		if (found in (offset until (offset + len))) return key
		offset += len
	}
	return weighted.keys.first()
}
