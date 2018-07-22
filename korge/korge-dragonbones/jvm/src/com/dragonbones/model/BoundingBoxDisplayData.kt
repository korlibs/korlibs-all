package com.dragonbones.model

import com.dragonbones.core.DisplayType

/**
 * @private
 */
class BoundingBoxDisplayData : DisplayData() {
    var boundingBox: BoundingBoxData? = null // Initial value.

    override fun _onClear() {
        super._onClear()

        if (this.boundingBox != null) {
            this.boundingBox!!.returnToPool()
        }

        this.type = DisplayType.BoundingBox
        this.boundingBox = null
    }
}
