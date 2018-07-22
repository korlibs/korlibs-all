package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.util.Array

import java.util.HashMap
import java.util.Objects

/**
 * 皮肤数据。（通常一个骨架数据至少包含一个皮肤数据）
 *
 * @version DragonBones 3.0
 * @language zh_CN
 */
class SkinData : BaseObject() {
    /**
     * 数据名称。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var name: String
    /**
     * @private
     */
    val displays: MutableMap<String, Array<DisplayData>> = HashMap()

    /**
     * @private
     */
    override fun _onClear() {
        for (k in this.displays.keys) {
            val slotDisplays = this.displays[k]
            for (display in slotDisplays) {
                if (display != null) {
                    display!!.returnToPool()
                }
            }

            this.displays.remove(k)
        }

        this.name = ""
        // this.displays.clear();
    }

    /**
     * @private
     */
    fun addDisplay(slotName: String, value: DisplayData?) {
        if (!this.displays.containsKey(slotName)) {
            this.displays[slotName] = Array()
        }

        val slotDisplays = this.displays[slotName] // TODO clear prev
        slotDisplays.add(value)
    }

    /**
     * @private
     */
    fun getDisplay(slotName: String, displayName: String): DisplayData? {
        val slotDisplays = this.getDisplays(slotName)
        if (slotDisplays != null) {
            for (display in slotDisplays) {
                if (display != null && display.name == displayName) {
                    return display
                }
            }
        }

        return null
    }

    /**
     * @private
     */
    fun getDisplays(slotName: String): Array<DisplayData>? {
        return if (!this.displays.containsKey(slotName)) {
            null
        } else this.displays[slotName]

    }
}
