package com.soywiz.kds

@Suppress("NOTHING_TO_INLINE")
data class Array2<T>(val width: Int, val height: Int, val data: Array<T>) : Iterable<T> {
	companion object {
		//inline operator fun <reified T> invoke(width: Int, height: Int, gen: () -> T) = Array2(width, height, Array(width * height) { gen() })
		inline operator fun <reified T> invoke(width: Int, height: Int, gen: (n: Int) -> T) = Array2(width, height, Array(width * height) { gen(it) })

		//inline operator fun <reified T> invoke(width: Int, height: Int, gen: (x: Int, y: Int) -> T) = Array2(width, height, Array(width * height) { gen(it % width, it / width) })
		inline operator fun <reified T> invoke(rows: List<List<T>>): Array2<T> {
			val width = rows[0].size
			val height = rows.size
			val anyCell = rows[0][0]
			return Array2(width, height) { anyCell }.apply { set(rows) }
		}

		inline operator fun <reified T> invoke(map: String, marginChar: Char = '\u0000', gen: (char: Char, x: Int, y: Int) -> T): Array2<T> {
			val lines = map.lines()
				.map {
					val res = it.trim()
					if (res.startsWith(marginChar)) {
						res.substring(0, res.length)
					} else {
						res
					}
				}
				.filter { it.isNotEmpty() }
			val width = lines.map { it.length }.max() ?: 0
			val height = lines.size

			return Array2(width, height) { n ->
				val x = n % width
				val y = n / width
				gen(lines.getOrNull(y)?.getOrNull(x) ?: ' ', x, y)
			}
		}

		inline operator fun <reified T> invoke(map: String, default: T, transform: Map<Char, T>): Array2<T> {
			return invoke(map) { c, x, y -> transform[c] ?: default }
		}

		inline fun <reified T> fromString(maps: Map<Char, T>, default: T, code: String, marginChar: Char = '\u0000'): Array2<T> {
			return invoke(code, marginChar = marginChar) { c, x, y -> maps[c] ?: default }
		}
	}

	fun set(rows: List<List<T>>) {
		var n = 0
		for (y in rows.indices) {
			val row = rows[y]
			for (x in row.indices) {
				this.data[n++] = row[x]
			}
		}
	}

	override fun equals(other: Any?): Boolean {
		return (other is Array2<*>) && this.width == other.width && this.height == other.height && this.data.contentEquals(other.data)
	}

	override fun hashCode(): Int = width + height + data.hashCode()

	private inline fun index(x: Int, y: Int) = y * width + x

	operator fun get(x: Int, y: Int): T = data[index(x, y)]
	operator fun set(x: Int, y: Int, value: T): Unit = run { data[index(x, y)] = value }

	fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height

	operator fun contains(v: T): Boolean = this.data.contains(v)

	inline fun each(callback: (x: Int, y: Int, v: T) -> Unit) {
		var n = 0
		for (y in 0 until height) {
			for (x in 0 until width) {
				callback(x, y, data[n++])
			}
		}
	}

	inline fun fill(gen: (old: T) -> T) {
		var n = 0
		for (y in 0 until height) {
			for (x in 0 until width) {
				data[n] = gen(data[n])
				n++
			}
		}
	}

	inline fun <reified TR> map2(gen: (x: Int, y: Int, v: T) -> TR): Array2<TR> = Array2<TR>(width, height) {
		val x = it % width
		val y = it / width
		//println("$it: ($x, $y), ($width, $height)")
		gen(x, y, this[x, y])
	}

	fun getPositionsWithValue(value: T) = data.indices.filter { data[it] == value }.map { Pair(it % width, it / width) }

	fun clone() = Array2(width, height, data.copyOf())

	fun dump() {
		for (y in 0 until height) {
			for (x in 0 until width) {
				print(this[x, y])
			}
			println()
		}
	}

	override fun iterator(): Iterator<T> = data.iterator()

	fun toStringList(charMap: (T) -> Char, margin: String = ""): List<String> {
		return (0 until height).map { y ->
			margin + (0 until width)
				.map { x -> charMap(this[x, y]) }
				.toCharArray() // Required for Kotlin.JS (or it will concatenate integers)
				.joinToString("")
		}
	}

	fun toString(margin: String = "", charMap: (T) -> Char): String = toStringList(charMap, margin = margin).joinToString("\n")

	fun toString(map: Map<T, Char>, margin: String = ""): String = toString(margin = margin) { map[it] ?: ' ' }
}