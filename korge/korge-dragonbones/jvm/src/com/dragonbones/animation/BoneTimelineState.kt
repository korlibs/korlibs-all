package com.dragonbones.animation

import com.dragonbones.armature.Bone

/**
 * @internal
 * @private
 */
abstract class BoneTimelineState : TweenTimelineState() {
    var bone: Bone? = null
    var bonePose: BonePose? = null

    override fun _onClear() {
        super._onClear()

        this.bone = null //
        this.bonePose = null //
    }
}
