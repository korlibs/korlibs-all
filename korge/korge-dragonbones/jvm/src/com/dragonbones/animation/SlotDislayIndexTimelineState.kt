package com.dragonbones.animation

/**
 * @internal
 * @private
 */
class SlotDislayIndexTimelineState : SlotTimelineState() {
    override fun _onArriveAtFrame() {
        if (this.playState >= 0) {
            val displayIndex =
                if (this._timelineData != null) this._frameArray!!.get(this._frameOffset + 1) else this.slot!!.slotData!!.displayIndex
            if (this.slot!!.displayIndex != displayIndex) {
                this.slot!!._setDisplayIndex(displayIndex, true)
            }
        }
    }
}
