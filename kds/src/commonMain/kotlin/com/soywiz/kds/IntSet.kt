package com.soywiz.kds

class IntSet {
	private val data = IntMap<Boolean>()

	fun clear() = run { data.clear() }
	fun add(item: Int) = run { data[item] = true }
	fun contains(item: Int) = data[item] == true
	fun remove(item: Int) = run { data.remove(item) }

	operator fun plusAssign(value: Int) = run { add(value); Unit }
	operator fun minusAssign(value: Int) = run { remove(value); Unit }

	override fun toString(): String {
		val entries = IntArrayList()
		for (e in data.entries) entries += e.key
		return "IntSet[${entries.joinToString(", ")}]"
	}
}