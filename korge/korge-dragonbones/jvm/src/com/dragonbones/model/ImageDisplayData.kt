package com.dragonbones.model

import com.dragonbones.core.DisplayType
import com.dragonbones.geom.Point

/**
 * @private
 */
open class ImageDisplayData : DisplayData() {
    val pivot = Point()
    var texture: TextureData? = null

    override fun _onClear() {
        super._onClear()

        this.type = DisplayType.Image
        this.pivot.clear()
        this.texture = null
    }
}
