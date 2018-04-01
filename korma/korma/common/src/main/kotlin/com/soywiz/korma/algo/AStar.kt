package com.soywiz.korma.algo

import com.soywiz.kds.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.*

object AStar {
	fun find(
		board: Array2<Boolean>, x0: Int, y0: Int, x1: Int, y1: Int, findClosest: Boolean = false,
		diagonals: Boolean = false
	): List<PointInt> {
		return find(board, x0, y0, x1, y1, { it }, findClosest, diagonals)
	}

	fun <T> find(
		board: Array2<T>,
		x0: Int,
		y0: Int,
		x1: Int,
		y1: Int,
		isBlocking: (v: T) -> Boolean,
		findClosest: Boolean = false,
		diagonals: Boolean = false
	): List<PointInt> {
		val aboard = board.map2 { x, y, value -> ANode(PointInt(x, y), isBlocking(value)) }
		val queue: PriorityQueue<ANode> = PriorityQueue { a, b -> a.weight - b.weight }

		val first = aboard.get(x0, y0)
		val dest = aboard.get(x1, y1)
		var closest = first
		var closestDist = Math.distance(x0, y0, x1, y1)
		if (!first.value) {
			queue.add(first)
			first.weight = 0
		}

		while (queue.isNotEmpty()) {
			val last = queue.remove()
			val dist = Math.distance(last.pos, dest.pos)
			if (dist < closestDist) {
				closestDist = dist
				closest = last
			}
			val nweight = last.weight + 1
			for (n in last.neightborhoods(aboard, diagonals)) {
				//trace(n);
				if (nweight < n.weight) {
					n.prev = last
					queue.add(n)
					n.weight = nweight
				}
			}
		}

		val route = arrayListOf<PointInt>()
		if (findClosest || closest == dest) {
			var current: ANode? = closest
			while (current != null) {
				route += current.pos
				current = current.prev
			}
			route.reverse()
		}

		return route
	}

	private class ANode(val pos: PointInt, val value: Boolean) {
		var visited = false
		var weight = 999999999
		var prev: ANode? = null

		fun neightborhoods(board: Array2<ANode>, diagonals: Boolean): List<ANode> {
			val out = arrayListOf<ANode>()
			fun add(dx: Int, dy: Int) {
				val x = this.pos.x + dx
				val y = this.pos.y + dy
				if (board.inside(x, y) && !board[x, y].value) out += board[x, y]
			}
			add(-1, 0)
			add(+1, 0)
			add(0, -1)
			add(0, +1)
			if (diagonals) {
				add(-1, -1)
				add(+1, -1)
				add(-1, +1)
				add(+1, +1)
			}
			return out
		}
	}
}
