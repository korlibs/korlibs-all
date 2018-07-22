package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.util.Array
import com.dragonbones.util.FloatArray
import com.dragonbones.util.IntArray

/**
 * 自定义数据。
 *
 * @version DragonBones 5.0
 * @language zh_CN
 */
class UserData : BaseObject() {
    /**
     * 自定义整数。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    val ints = IntArray()
    /**
     * 自定义浮点数。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    val floats = FloatArray()
    /**
     * 自定义字符串。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    val strings = Array<String>()

    /**
     * 获取自定义整数。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    val int: Int
        get() = getInt(0)

    /**
     * 获取自定义浮点数。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    val float: Float
        get() = getFloat(0)

    /**
     * 获取自定义字符串。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    val string: String
        get() = getString(0)

    /**
     * @private
     */
    override fun _onClear() {
        this.ints.clear()
        this.floats.clear()
        this.strings.clear()
    }

    /**
     * 获取自定义整数。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    fun getInt(index: Int): Int {
        return if (index >= 0 && index < this.ints.size()) this.ints.get(index) else 0
    }

    /**
     * 获取自定义浮点数。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    fun getFloat(index: Int): Float {
        return if (index >= 0 && index < this.floats.size()) this.floats.get(index) else 0f
    }

    /**
     * 获取自定义字符串。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    fun getString(index: Int): String {
        return if (index >= 0 && index < this.strings.size()) this.strings.get(index) else ""
    }
}
