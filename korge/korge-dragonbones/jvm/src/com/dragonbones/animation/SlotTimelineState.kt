package com.dragonbones.animation

import com.dragonbones.armature.Slot

/**
 * @internal
 * @private
 */
abstract class SlotTimelineState : TweenTimelineState() {
    var slot: Slot? = null

    override fun _onClear() {
        super._onClear()

        this.slot = null //
    }
}
