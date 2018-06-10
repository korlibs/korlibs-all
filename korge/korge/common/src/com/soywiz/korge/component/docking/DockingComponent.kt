package com.soywiz.korge.component.docking

import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*

class DockingComponent(view: View, var anchor: Anchor) : Component(view) {
	//private val bounds = Rectangle()

	init {
		addEventListener<StageResizedEvent> { e ->
			view.x = views.actualVirtualLeft.toDouble() + (views.actualVirtualWidth) * anchor.sx
			view.y = views.actualVirtualTop.toDouble() + (views.actualVirtualHeight) * anchor.sy
			view.invalidate()
			view.parent?.invalidate()
		}
	}
}

fun <T : View> T.dockedTo(anchor: Anchor) = this.apply { DockingComponent(this, anchor).attach() }
