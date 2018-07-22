package com.dragonbones.model

import com.dragonbones.core.BaseObject

/**
 * @private
 */
abstract class ConstraintData : BaseObject() {
    var order: Float = 0.toFloat()
    var target: BoneData? = null
    var bone: BoneData? = null
    var root: BoneData? = null

    override fun _onClear() {
        this.order = 0f
        this.target = null //
        this.bone = null //
        this.root = null
    }
}
