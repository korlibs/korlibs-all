package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.core.TimelineType

/**
 * @private
 */
class TimelineData : BaseObject() {
    var type: TimelineType
    var offset: Int = 0 // TimelineArray.
    var frameIndicesOffset: Int = 0 // FrameIndices.

    override fun _onClear() {
        this.type = TimelineType.BoneAll
        this.offset = 0
        this.frameIndicesOffset = -1
    }
}
