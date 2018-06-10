package com.soywiz.kds

// @TODO: Requires more work
class LinkedList<T>(private val debug: Boolean) : MutableCollection<T> {
	//class LinkedList<T>(private val debug: Boolean) {
	constructor() : this(false)

	companion object {
		const private val NONE = -1
	}

	private var firstSlot = NONE
	private var lastSlot = NONE

	private var firstFreeSlot = 0
	private var lastFreeSlot = 15

	override var size: Int = 0; private set

	private var prev = IntArray(16) { it - 1 }
	private var next = IntArray(16) { it + 1 }
	private var items = arrayOfNulls<Any>(16) as Array<T>

	private val capacity: Int get() = items.size

	init {
		prev[0] = NONE
		next[next.size - 1] = NONE
		checkInternalState()
	}

	operator fun get(index: Int): T {
		if (index < 0 || index >= size) throw IndexOutOfBoundsException()
		iterate { cindex, slot -> if (cindex == index) return items[slot] }
		throw IllegalStateException()
	}

	operator override fun contains(item: T) = indexOf(item) != NONE

	val first get() = items.getOrNull(firstSlot)
	val last get() = items.getOrNull(lastSlot)

	private fun ensure(count: Int) {
		val oldCapacity = capacity
		if (size + count >= oldCapacity) {
			val newCapacity = oldCapacity * 4
			prev = prev.copyOf(newCapacity)
			next = next.copyOf(newCapacity)
			items = items.copyOf(newCapacity) as Array<T>
			for (n in (oldCapacity + 1) until newCapacity) prev[n] = n - 1
			for (n in oldCapacity until newCapacity) next[n] = n + 1
			prev[oldCapacity] = lastFreeSlot
			next[lastFreeSlot] = oldCapacity
			next[newCapacity - 1] = NONE
			lastFreeSlot = newCapacity - 1
		}
	}

	/**
	 * @return int Slot for fast addition
	 */
	fun addLast(item: T): Int {
		val slot = allocateSlot()
		if (lastSlot != NONE) next[lastSlot] = slot
		next[slot] = NONE
		prev[slot] = lastSlot
		items[slot] = item
		if (firstSlot == NONE) firstSlot = slot
		lastSlot = slot
		size++
		checkInternalState()
		return slot
	}

	/**
	 * @return int Slot for fast addition
	 */
	fun addFirst(item: T): Int {
		val slot = allocateSlot()
		if (firstSlot != NONE) prev[firstSlot] = slot
		prev[slot] = NONE
		next[slot] = firstSlot
		items[slot] = item
		if (lastSlot == NONE) lastSlot = slot
		firstSlot = slot
		size++
		checkInternalState()
		return slot
	}

	private fun allocateSlot(): Int {
		ensure(+1)
		val slot = firstFreeSlot
		firstFreeSlot = next[firstFreeSlot]
		if (firstFreeSlot == NONE) {
			throw IllegalStateException()
		}
		prev[firstFreeSlot] = NONE
		return slot
	}


	private fun freeSlot(slot: Int) {
		prev[firstFreeSlot] = slot
		next[slot] = firstFreeSlot
		prev[slot] = NONE
		firstFreeSlot = slot
		checkInternalState()
	}

	fun addAt(index: Int, item: T): Int {
		if (index == 0) return addFirst(item)
		if (index == size) return addLast(item)
		return addBeforeSlot(slotOfIndex(index), item)
	}

	fun addBeforeSlot(slot: Int, item: T): Int {
		if (slot == NONE) throw IllegalArgumentException()
		val newSlot = allocateSlot()

		items[newSlot] = item

		prev[newSlot] = prev[slot]
		next[newSlot] = slot

		if (prev[slot] != NONE) {
			next[prev[slot]] = newSlot
		}

		prev[slot] = newSlot

		if (firstSlot == slot) {
			firstSlot = newSlot
		}

		size++

		checkInternalState()
		return newSlot
	}

	fun addAfterSlot(slot: Int, item: T): Int {
		val newSlot = allocateSlot()

		next[newSlot] = next[slot]
		prev[newSlot] = slot

		if (next[slot] != NONE) {
			prev[next[slot]] = newSlot
		}

		next[slot] = newSlot

		items[newSlot] = item

		if (lastSlot == slot) {
			lastSlot = newSlot
		}

		size++

		checkInternalState()
		return newSlot
	}

	fun indexOf(item: T): Int {
		iterate { cindex, cslot -> if (items[cslot] == item) return cindex }
		return NONE
	}

	fun slotOf(item: T): Int {
		iterate { _, cslot -> if (items[cslot] == item) return cslot }
		return NONE
	}

	fun slotOfIndex(index: Int): Int {
		iterate { cindex, cslot -> if (cindex == index) return cslot }
		return NONE
	}

	override fun remove(item: T): Boolean {
		val slot = slotOf(item)
		if (slot != NONE) removeSlot(slot)
		return slot != NONE
	}

	fun removeAt(index: Int): T {
		if (index < 0 || index >= size) throw IndexOutOfBoundsException()
		if (index >= size / 2) {
			iterateReverse { cindex, cslot -> if (cindex == index) return removeSlot(cslot) }
		} else {
			iterate { cindex, cslot -> if (cindex == index) return removeSlot(cslot) }
		}
		throw IllegalStateException()
	}

	fun removeFirst() = removeSlot(firstSlot)
	fun removeLast() = removeSlot(lastSlot)

	fun removeSlot(slot: Int): T {
		if (slot < 0 || slot >= capacity) throw IndexOutOfBoundsException()
		if (firstSlot == slot) firstSlot = next[slot]
		if (lastSlot == slot) lastSlot = prev[slot]
		val p = prev[slot]
		val n = next[slot]
		if (p != NONE) next[p] = n
		if (n != NONE) prev[n] = p
		size--
		freeSlot(slot)
		checkInternalState()
		return items[slot]
	}

	private inline fun iterate(startSlot: Int = this.firstSlot, callback: (cindex: Int, cslot: Int) -> Unit) {
		var cindex = 0
		var cslot = startSlot
		while (cslot != NONE) {
			callback(cindex, cslot)
			cslot = next[cslot]
			cindex++
		}
	}

	private inline fun iterateReverse(startSlot: Int = this.lastSlot, callback: (cindex: Int, cslot: Int) -> Unit) {
		var cindex = size - 1
		var cslot = startSlot
		while (cslot != NONE) {
			callback(cindex, cslot)
			cslot = prev[cslot]
			cindex--
		}
	}

	override fun removeAll(elements: Collection<T>): Boolean = _removeRetainAll(elements, retain = false)
	override fun retainAll(elements: Collection<T>): Boolean = _removeRetainAll(elements, retain = true)

	private fun _removeRetainAll(elements: Collection<T>, retain: Boolean): Boolean {
		val eset = elements.toSet()
		val temp = arrayListOf<T>()
		iterate { cindex, cslot -> if ((items[cslot] in eset) == retain) temp += items[cslot] }
		if (temp.size == this.size) return false
		clear()
		for (e in temp) addLast(e)
		checkInternalState()
		return true
	}

	override fun containsAll(elements: Collection<T>): Boolean {
		val emap = elements.map { it to 0 }.toLinkedMap()
		iterate { cindex, cslot ->
			val e = items[cslot]
			if (e in emap) emap[e] = 1
		}
		checkInternalState()
		return emap.values.all { it == 1 }
	}

	override fun isEmpty(): Boolean = size == 0
	override fun add(element: T): Boolean = true.apply { addLast(element) }
	override fun addAll(elements: Collection<T>): Boolean =
		true.apply { ensure(elements.size); for (e in elements) addLast(e) }

	override fun clear() {
		firstSlot = -1
		lastSlot = -1
		firstFreeSlot = 0
		size = 0
		for (n in prev.indices) {
			prev[n] = if (n == 0) -1 else n - 1
			next[n] = if (n == prev.size - 1) -1 else n + 1
		}
	}

	override operator fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
		var cslot = firstSlot
		private var lastCslot = cslot

		override operator fun hasNext(): Boolean = cslot != NONE

		override operator fun next(): T {
			lastCslot = cslot
			return items[cslot].apply { cslot = next[cslot] }
		}

		override fun remove() {
			removeSlot(lastCslot)
		}
	}

	private fun checkInternalState() {
		if (debug) checkInternalStateFull()
	}

	private fun checkInternalStateFull() {
		val slots = _getAllocatedSlots()
		val slotsReversed = _getAllocatedSlotsReverse().reversed()
		val freeSlots = _getFreeSlots()
		val freeSlotsReverse = _getFreeSlotsReverse().reversed()
		if (slots != slotsReversed) {
			throw IllegalStateException()
		}
		if (freeSlots != freeSlotsReverse) {
			throw IllegalStateException()
		}
		if (slots.size != size) {
			throw IllegalStateException()
		}
		if (slots.size + freeSlots.size != capacity) {
			throw IllegalStateException()
		}
	}

	private fun _getAllocatedSlots(): List<Int> {
		val slots = arrayListOf<Int>()
		iterate(firstSlot) { _, cslot -> slots += cslot }
		return slots
	}

	private fun _getAllocatedSlotsReverse(): List<Int> {
		val slots = arrayListOf<Int>()
		iterateReverse(lastSlot) { _, cslot -> slots += cslot }
		return slots
	}

	private fun _getFreeSlots(): List<Int> {
		val slots = arrayListOf<Int>()
		iterate(firstFreeSlot) { _, cslot -> slots += cslot }
		return slots
	}

	private fun _getFreeSlotsReverse(): List<Int> {
		val slots = arrayListOf<Int>()
		iterateReverse(lastFreeSlot) { _, cslot -> slots += cslot }
		return slots
	}
}
