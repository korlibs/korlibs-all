package com.dragonbones.model

import com.dragonbones.core.DisplayType
import com.dragonbones.util.Array

/**
 * @private
 */
class ArmatureDisplayData : DisplayData() {
    var inheritAnimation: Boolean = false
    val actions = Array<ActionData>()
    var armature: ArmatureData? = null

    override fun _onClear() {
        super._onClear()

        for (action in this.actions) {
            action.returnToPool()
        }

        this.type = DisplayType.Armature
        this.inheritAnimation = false
        this.actions.clear()
        this.armature = null
    }
}
