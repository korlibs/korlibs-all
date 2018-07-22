package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.util.Console

import java.util.HashMap

/**
 * 贴图集数据。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 */
abstract class TextureAtlasData : BaseObject() {
    /**
     * 是否开启共享搜索。
     *
     * @default false
     * @version DragonBones 4.5
     * @language zh_CN
     */
    var autoSearch: Boolean = false
    /**
     * @private
     */
    var width: Int = 0
    /**
     * @private
     */
    var height: Int = 0
    /**
     * 贴图集缩放系数。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var scale: Float = 0.toFloat()
    /**
     * 贴图集名称。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var name: String
    /**
     * 贴图集图片路径。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var imagePath: String
    /**
     * @private
     */
    val textures: MutableMap<String, TextureData> = HashMap()

    /**
     * @private
     */
    override fun _onClear() {
        for (k in this.textures.keys) {
            this.textures[k].returnToPool()
            this.textures.remove(k)
        }

        this.autoSearch = false
        this.width = 0
        this.height = 0
        this.scale = 1f
        // this.textures.clear();
        this.name = ""
        this.imagePath = ""
    }

    /**
     * @private
     */
    fun copyFrom(value: TextureAtlasData) {
        this.autoSearch = value.autoSearch
        this.scale = value.scale
        this.width = value.width
        this.height = value.height
        this.name = value.name
        this.imagePath = value.imagePath

        for (k in this.textures.keys) {
            this.textures[k].returnToPool()
            this.textures.remove(k)
        }

        // this.textures.clear();

        for (k in value.textures.keys) {
            val texture = this.createTexture()
            texture.copyFrom(value.textures[k])
            this.textures[k] = texture
        }
    }

    /**
     * @private
     */
    abstract fun createTexture(): TextureData

    /**
     * @private
     */
    fun addTexture(value: TextureData) {
        if (this.textures.containsKey(value.name)) {
            Console.warn("Replace texture: " + value.name)
            this.textures[value.name].returnToPool()
        }

        value.parent = this
        this.textures[value.name] = value
    }

    /**
     * @private
     */
    fun getTexture(name: String): TextureData? {
        return this.textures[name]
    }
}
