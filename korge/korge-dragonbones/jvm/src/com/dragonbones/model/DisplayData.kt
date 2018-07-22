package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.core.DisplayType
import com.dragonbones.geom.Transform

/**
 * @private
 */
abstract class DisplayData : BaseObject() {
    var type: DisplayType? = null
    var name: String
    var path: String
    val transform = Transform()
    var parent: ArmatureData? = null

    override fun _onClear() {
        this.name = ""
        this.path = ""
        this.transform.identity()
        this.parent = null //
    }
}

