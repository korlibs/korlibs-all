package com.soywiz.kds

/**
 * @TODO Optimize!!
 */
class PriorityQueue<T>(
	private var compare: Comparator<T>,
	private var reversed: Boolean = false
) : MutableCollection<T> {
	constructor(reversed: Boolean = false, compare: (T, T) -> Int)
		: this(Comparator(compare), reversed)

	private var dirtyList = ArrayList<T>()

	private var dirty: Boolean = false

	val _sortedList: ArrayList<T>
		get() {
			if (dirty) {
				dirtyList.sortWith(compare)
				dirty = false
			}
			return dirtyList
		}

	override val size: Int get() = dirtyList.size
	val length: Int get() = dirtyList.size

	fun updateObject(obj: T): Unit {
		dirty = true
	}

	override fun contains(element: T): Boolean {
		return this.dirtyList.indexOf(element) != -1
	}

	fun push(obj: T): Unit {
		dirtyList.add(obj)
		dirty = true
	}

	override fun add(element: T): Boolean {
		dirty = true
		return dirtyList.add(element)
	}

	override fun addAll(elements: Collection<T>): Boolean {
		dirty = true
		return dirtyList.addAll(elements)
	}

	override fun clear() {
		dirty = true
		dirtyList.clear()
	}

	override fun isEmpty(): Boolean = dirtyList.isEmpty()

	override fun iterator(): MutableIterator<T> {
		return _sortedList.iterator()
	}

	override fun remove(element: T): Boolean {
		return _sortedList.remove(element)
	}

	override fun retainAll(elements: Collection<T>): Boolean {
		return _sortedList.retainAll(elements)
	}

	override fun containsAll(elements: Collection<T>): Boolean {
		return _sortedList.containsAll(elements)
	}

	override fun removeAll(elements: Collection<T>): Boolean {
		return _sortedList.removeAll(elements)
	}

	fun add(vararg objs: T): Unit {
		dirtyList.addAll(objs)
		dirty = true
	}

	fun add(objs: Iterable<T>): Unit {
		dirtyList.addAll(objs)
		dirty = true
	}

	val head: T get() = _sortedList[if (this.reversed) (_sortedList.size - 1) else 0]

	fun removeHead(): T {
		if (this.reversed) {
			return _sortedList.removeAt(_sortedList.size - 1)
		} else {
			return _sortedList.removeAt(0)
		}
	}

	// @TODO: Verify
	fun remove(): T = removeHead()
}
