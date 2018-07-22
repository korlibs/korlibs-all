package com.dragonbones.model

import com.dragonbones.core.BaseObject

/**
 * @private
 */
class CanvasData : BaseObject() {
    var hasBackground: Boolean = false
    var color: Int = 0
    var x: Float = 0.toFloat()
    var y: Float = 0.toFloat()
    var width: Float = 0.toFloat()
    var height: Float = 0.toFloat()

    /**
     * @private
     */
    override fun _onClear() {
        this.hasBackground = false
        this.color = 0x000000
        this.x = 0f
        this.y = 0f
        this.width = 0f
        this.height = 0f
    }
}