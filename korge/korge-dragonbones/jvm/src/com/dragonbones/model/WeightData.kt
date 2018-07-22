package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.util.Array

/**
 * @private
 */
class WeightData : BaseObject() {
    var count: Int = 0
    var offset: Int = 0 // IntArray.
    val bones = Array<BoneData>()

    override fun _onClear() {
        this.count = 0
        this.offset = 0
        this.bones.clear()
    }
}
