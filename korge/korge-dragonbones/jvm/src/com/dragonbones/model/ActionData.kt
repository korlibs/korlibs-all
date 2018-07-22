package com.dragonbones.model

import com.dragonbones.core.ActionType
import com.dragonbones.core.BaseObject

/**
 * @private
 */
class ActionData : BaseObject() {
    var type: ActionType
    var name: String // Frame event name | Sound event name | Animation name
    var bone: BoneData? = null
    var slot: SlotData? = null
    var data: UserData? = null //

    override fun _onClear() {
        if (this.data != null) {
            this.data!!.returnToPool()
        }

        this.type = ActionType.Play
        this.name = ""
        this.bone = null
        this.slot = null
        this.data = null
    }
}
