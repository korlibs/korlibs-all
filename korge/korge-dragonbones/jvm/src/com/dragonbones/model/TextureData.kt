package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.geom.Rectangle

/**
 * @private
 */
abstract class TextureData : BaseObject() {

    var rotated: Boolean = false
    var name: String
    val region = Rectangle()
    var parent: TextureAtlasData? = null
    var frame: Rectangle? = null // Initial value.

    override fun _onClear() {
        this.rotated = false
        this.name = ""
        this.region.clear()
        this.parent = null //
        this.frame = null
    }

    fun copyFrom(value: TextureData) {
        this.rotated = value.rotated
        this.name = value.name
        this.region.copyFrom(value.region)
        this.parent = value.parent

        if (this.frame == null && value.frame != null) {
            this.frame = TextureData.createRectangle()
        } else if (this.frame != null && value.frame == null) {
            this.frame = null
        }

        if (this.frame != null && value.frame != null) {
            this.frame!!.copyFrom(value.frame)
        }
    }

    companion object {
        fun createRectangle(): Rectangle {
            return Rectangle()
        }
    }
}
