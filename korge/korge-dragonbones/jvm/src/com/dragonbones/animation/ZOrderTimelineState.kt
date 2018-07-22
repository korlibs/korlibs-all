package com.dragonbones.animation

/**
 * @internal
 * @private
 */
class ZOrderTimelineState : TimelineState() {
    override fun _onArriveAtFrame() {
        if (this.playState >= 0) {
            val count = this._frameArray!!.get(this._frameOffset + 1)
            if (count > 0) {
                this._armature!!._sortZOrder(this._frameArray, this._frameOffset + 2)
            } else {
                this._armature!!._sortZOrder(null, 0)
            }
        }
    }

    override fun _onUpdateFrame() {}
}
