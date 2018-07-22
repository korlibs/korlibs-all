package com.dragonbones.model

import com.dragonbones.core.DisplayType

/**
 * @private
 */
class MeshDisplayData : ImageDisplayData() {
    var inheritAnimation: Boolean = false
    var offset: Int = 0 // IntArray.
    var weight: WeightData? = null // Initial value.

    override fun _onClear() {
        super._onClear()

        if (this.weight != null) {
            this.weight!!.returnToPool()
        }

        this.type = DisplayType.Mesh
        this.inheritAnimation = false
        this.offset = 0
        this.weight = null
    }
}
