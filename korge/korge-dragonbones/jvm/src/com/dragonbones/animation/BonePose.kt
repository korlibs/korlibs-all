package com.dragonbones.animation

import com.dragonbones.core.BaseObject
import com.dragonbones.geom.Transform

/**
 * @internal
 * @private
 */
class BonePose : BaseObject() {
    val current = Transform()
    val delta = Transform()
    val result = Transform()

    override fun _onClear() {
        this.current.identity()
        this.delta.identity()
        this.result.identity()
    }
}
