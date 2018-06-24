package com.soywiz.korge.component.docking

import com.soywiz.korge.component.*
import com.soywiz.korge.view.*

class SortedChildrenByComponent(val container: Container, var comparator: Comparator<View>) : Component(container) {
	override fun update(dtMs: Int) {
		super.update(dtMs)
		container.children.sortWith(comparator)
	}
}

// @TODO: kotlin-native: kotlin.Comparator { }
//             korge/korge/common/src/com/soywiz/korge/component/docking/SortedChildrenByComponent.kt:25:127: error: unresolved reference: Comparator
// @TODO: kotlin-native: recursive problem!
//korge/korge/common/src/com/soywiz/korge/component/docking/SortedChildrenByComponent.kt:18:140: error: cannot infer a type for this parameter. Please specify it explicitly.
//      fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2): T = this.keepChildrenSortedBy(kotlin.Comparator { a, b -> selector(a).compareTo(selector(b)) })

//fun <T : Container> T.keepChildrenSortedBy(comparator: Comparator<View>) = this.apply { SortedChildrenByComponent(this, comparator).attach() }
//fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2) = this.keepChildrenSortedBy(kotlin.Comparator { a, b -> selector(a).compareTo(selector(b)) })

//fun <T : Container> T.keepChildrenSortedBy(comparator: Comparator<View>): T = this.apply { SortedChildrenByComponent(this, comparator).attach() }
//fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2): T = this.keepChildrenSortedBy(kotlin.Comparator { a, b -> selector(a).compareTo(selector(b)) })
//fun <T : Container> T.keepChildrenSortedByY(): T = this.keepChildrenSortedBy(View::y)
//fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2): T = this.keepChildrenSortedBy(kotlin.Comparator { a: View, b: View -> selector(a).compareTo(selector(b)) })

fun <T : Container> T.keepChildrenSortedBy(comparator: Comparator<View>): T =
	this.apply { SortedChildrenByComponent(this, comparator).attach() }

fun <T : Container, T2 : Comparable<T2>> T.keepChildrenSortedBy(selector: (View) -> T2): T =
	this.keepChildrenSortedBy(Comparator { a: View, b: View -> selector(a).compareTo(selector(b)) })

fun <T : Container> T.keepChildrenSortedByY(): T = this.keepChildrenSortedBy(View::y)
