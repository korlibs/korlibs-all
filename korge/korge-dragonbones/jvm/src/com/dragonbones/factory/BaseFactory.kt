package com.dragonbones.factory

import com.dragonbones.armature.Armature
import com.dragonbones.armature.Bone
import com.dragonbones.armature.IKConstraint
import com.dragonbones.armature.Slot
import com.dragonbones.core.BaseObject
import com.dragonbones.core.DisplayType
import com.dragonbones.core.DragonBones
import com.dragonbones.model.*
import com.dragonbones.parser.BinaryDataParser
import com.dragonbones.parser.DataParser
import com.dragonbones.parser.ObjectDataParser
import com.dragonbones.util.Array
import com.dragonbones.util.Console

import java.util.HashMap
import java.util.Objects

/**
 * 创建骨架的基础工厂。 (通常只需要一个全局工厂实例)
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see DragonBonesData
 *
 * @see TextureAtlasData
 *
 * @see ArmatureData
 *
 * @see Armature
 */
abstract class BaseFactory
/**
 * 创建一个工厂。 (通常只需要一个全局工厂实例)
 *
 * @param dataParser 龙骨数据解析器，如果不设置，则使用默认解析器。
 * @version DragonBones 3.0
 * @language zh_CN
 */
@JvmOverloads constructor(dataParser: DataParser? = null) {
    /**
     * 是否开启共享搜索。
     * 如果开启，创建一个骨架时，可以从多个龙骨数据中寻找骨架数据，或贴图集数据中寻找贴图数据。 (通常在有共享导出的数据时开启)
     *
     * @version DragonBones 4.5
     * @language zh_CN
     * @see DragonBonesData.autoSearch
     *
     * @see com.dragonbones.model.TextureAtlasData.autoSearch
     */
    var autoSearch = false
    /**
     * @private
     */
    protected val _dragonBonesDataMap: MutableMap<String, DragonBonesData> = HashMap()
    /**
     * @private
     */
    protected val _textureAtlasDataMap: MutableMap<String, Array<TextureAtlasData>> = HashMap()
    /**
     * @private
     */
    protected var _dragonBones: DragonBones? = null
    /**
     * @private
     */
    protected var _dataParser: DataParser? = null

    /**
     * @private
     */
    val allDragonBonesData: Map<String, DragonBonesData>
        get() = this._dragonBonesDataMap

    /**
     * @private
     */
    val allTextureAtlasData: Map<String, Array<TextureAtlasData>>
        get() = this._textureAtlasDataMap

    init {
        if (BaseFactory._objectParser == null) {
            BaseFactory._objectParser = ObjectDataParser()
        }

        if (BaseFactory._binaryParser == null) {
            BaseFactory._binaryParser = BinaryDataParser()
        }

        this._dataParser = dataParser ?: BaseFactory._objectParser
    }

    /**
     * @private
     */
    protected fun _isSupportMesh(): Boolean {
        return true
    }

    /**
     * @private
     */
    protected fun _getTextureData(textureAtlasName: String, textureName: String): TextureData? {
        if (this._textureAtlasDataMap.containsKey(textureAtlasName)) {
            for (textureAtlasData in this._textureAtlasDataMap[textureAtlasName]) {
                val textureData = textureAtlasData.getTexture(textureName)
                if (textureData != null) {
                    return textureData
                }
            }
        }

        if (this.autoSearch) { // Will be search all data, if the autoSearch is true.
            for (k in this._textureAtlasDataMap.keys) {
                for (textureAtlasData in this._textureAtlasDataMap[k]) {
                    if (textureAtlasData.autoSearch) {
                        val textureData = textureAtlasData.getTexture(textureName)
                        if (textureData != null) {
                            return textureData
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * @private
     */
    protected fun _fillBuildArmaturePackage(
        dataPackage: BuildArmaturePackage,
        dragonBonesName: String, armatureName: String, skinName: String, textureAtlasName: String
    ): Boolean {
        var dragonBonesName = dragonBonesName
        var dragonBonesData: DragonBonesData? = null
        var armatureData: ArmatureData? = null

        if (dragonBonesName.length > 0) {
            if (this._dragonBonesDataMap.containsKey(dragonBonesName)) {
                dragonBonesData = this._dragonBonesDataMap[dragonBonesName]
                armatureData = dragonBonesData!!.getArmature(armatureName)
            }
        }

        if (armatureData == null && (dragonBonesName.length == 0 || this.autoSearch)) { // Will be search all data, if do not give a data name or the autoSearch is true.
            for (k in this._dragonBonesDataMap.keys) {
                dragonBonesData = this._dragonBonesDataMap[k]
                if (dragonBonesName.length == 0 || dragonBonesData!!.autoSearch) {
                    armatureData = dragonBonesData!!.getArmature(armatureName)
                    if (armatureData != null) {
                        dragonBonesName = k
                        break
                    }
                }
            }
        }

        if (armatureData != null) {
            dataPackage.dataName = dragonBonesName
            dataPackage.textureAtlasName = textureAtlasName
            dataPackage.data = dragonBonesData
            dataPackage.armature = armatureData
            dataPackage.skin = null

            if (skinName.length > 0) {
                dataPackage.skin = armatureData.getSkin(skinName)
                if (dataPackage.skin == null && this.autoSearch) {
                    for (k in this._dragonBonesDataMap.keys) {
                        val skinDragonBonesData = this._dragonBonesDataMap[k]
                        val skinArmatureData = skinDragonBonesData.getArmature(skinName)
                        if (skinArmatureData != null) {
                            dataPackage.skin = skinArmatureData.defaultSkin
                            break
                        }
                    }
                }
            }

            if (dataPackage.skin == null) {
                dataPackage.skin = armatureData.defaultSkin
            }

            return true
        }

        return false
    }

    /**
     * @private
     */
    protected fun _buildBones(dataPackage: BuildArmaturePackage, armature: Armature) {
        val bones = dataPackage.armature!!.sortedBones
        for (i in 0 until bones.size()) {
            val boneData = bones.get(i)
            val bone = BaseObject.borrowObject(Bone::class.java)
            bone.init(boneData)

            if (boneData.parent != null) {
                armature.addBone(bone, boneData.parent!!.name)
            } else {
                armature.addBone(bone)
            }

            val constraints = boneData.constraints
            for (j in 0 until constraints.size()) {
                val constraintData = constraints.get(j)
                val target = armature.getBone(constraintData.target!!.name) ?: continue

                // TODO more constraint type.
                val ikConstraintData = constraintData as IKConstraintData
                val constraint = BaseObject.borrowObject(IKConstraint::class.java)
                val root = if (ikConstraintData.root != null) armature.getBone(ikConstraintData.root!!.name) else null
                constraint.target = target
                constraint.bone = bone
                constraint.root = root
                constraint.bendPositive = ikConstraintData.bendPositive
                constraint.scaleEnabled = ikConstraintData.scaleEnabled
                constraint.weight = ikConstraintData.weight

                if (root != null) {
                    root.addConstraint(constraint)
                } else {
                    bone.addConstraint(constraint)
                }
            }
        }
    }

    /**
     * @private
     */
    protected fun _buildSlots(dataPackage: BuildArmaturePackage, armature: Armature) {
        val currentSkin = dataPackage.skin
        val defaultSkin = dataPackage.armature!!.defaultSkin
        if (currentSkin == null || defaultSkin == null) {
            return
        }

        val skinSlots = HashMap<String, Array<DisplayData>>()
        for (k in defaultSkin.displays.keys) {
            val displays = defaultSkin.displays[k]
            skinSlots[k] = displays
        }

        if (currentSkin !== defaultSkin) {
            for (k in currentSkin.displays.keys) {
                val displays = currentSkin.displays[k]
                skinSlots[k] = displays
            }
        }

        for (slotData in dataPackage.armature!!.sortedSlots) {
            if (!skinSlots.containsKey(slotData.name)) {
                continue
            }

            val displays = skinSlots[slotData.name]
            val slot = this._buildSlot(dataPackage, slotData, displays, armature)
            val displayList = Array<Any>()
            for (displayData in displays) {
                if (displayData != null) {
                    displayList.push(this._getSlotDisplay(dataPackage, displayData, null, slot))
                } else {
                    displayList.push(null)
                }
            }

            armature.addSlot(slot, slotData.parent!!.name)
            slot._setDisplayList(displayList)
            slot._setDisplayIndex(slotData.displayIndex, true)
        }
    }

    /**
     * @private
     */
    protected fun _getSlotDisplay(
        dataPackage: BuildArmaturePackage?,
        displayData: DisplayData,
        rawDisplayData: DisplayData?,
        slot: Slot
    ): Any? {
        val dataName = if (dataPackage != null) dataPackage.dataName else displayData.parent!!.parent!!.name
        var display: Any? = null
        when (displayData.type) {
            DisplayType.Image -> {
                val imageDisplayData = displayData as ImageDisplayData
                if (imageDisplayData.texture == null) {
                    imageDisplayData.texture = this._getTextureData(dataName, displayData.path)
                } else if (dataPackage != null && dataPackage.textureAtlasName.length > 0) {
                    imageDisplayData.texture = this._getTextureData(dataPackage.textureAtlasName, displayData.path)
                }

                if (rawDisplayData != null && rawDisplayData.type == DisplayType.Mesh && this._isSupportMesh()) {
                    display = slot.meshDisplay
                } else {
                    display = slot.rawDisplay
                }
            }

            DisplayType.Mesh -> {
                val meshDisplayData = displayData as MeshDisplayData
                if (meshDisplayData.texture == null) {
                    meshDisplayData.texture = this._getTextureData(dataName, meshDisplayData.path)
                } else if (dataPackage != null && dataPackage.textureAtlasName.length > 0) {
                    meshDisplayData.texture = this._getTextureData(dataPackage.textureAtlasName, meshDisplayData.path)
                }

                if (this._isSupportMesh()) {
                    display = slot.meshDisplay
                } else {
                    display = slot.rawDisplay
                }
            }

            DisplayType.Armature -> {
                val armatureDisplayData = displayData as ArmatureDisplayData
                val childArmature =
                    this.buildArmature(armatureDisplayData.path, dataName, null, dataPackage?.textureAtlasName)
                if (childArmature != null) {
                    childArmature.inheritAnimation = armatureDisplayData.inheritAnimation
                    if (!childArmature.inheritAnimation) {
                        val actions =
                            if (armatureDisplayData.actions.size() > 0) armatureDisplayData.actions else childArmature.armatureData!!.defaultActions
                        if (actions.size() > 0) {
                            for (action in actions) {
                                childArmature._bufferAction(action, true)
                            }
                        } else {
                            childArmature.animation!!.play()
                        }
                    }

                    armatureDisplayData.armature = childArmature.armatureData //
                }

                display = childArmature
            }
        }

        return display
    }

    /**
     * @private
     */
    protected fun _replaceSlotDisplay(
        dataPackage: BuildArmaturePackage,
        displayData: DisplayData?,
        slot: Slot,
        displayIndex: Int
    ) {
        var displayIndex = displayIndex
        if (displayIndex < 0) {
            displayIndex = slot.displayIndex
        }

        if (displayIndex < 0) {
            displayIndex = 0
        }

        val displayList = slot.displayList // Copy.
        if (displayList.size() <= displayIndex) {
            displayList.length = displayIndex + 1

            // @TODO: Not required in java
            var i = 0
            val l = displayList.size()
            while (i < l) { // Clean undefined.
                if (displayList.get(i) == null) {
                    displayList.set(i, null)
                }
                ++i
            }
        }

        if (slot._displayDatas.size() <= displayIndex) {
            displayList.length = displayIndex + 1

            // @TODO: Not required in java
            var i = 0
            val l = slot._displayDatas.size()
            while (i < l) { // Clean undefined.
                if (slot._displayDatas.get(i) == null) {
                    slot._displayDatas.set(i, null)
                }
                ++i
            }
        }

        slot._displayDatas.set(displayIndex, displayData)
        if (displayData != null) {
            displayList.set(
                displayIndex, this._getSlotDisplay(
                    dataPackage,
                    displayData,
                    if (displayIndex < slot._rawDisplayDatas!!.size()) slot._rawDisplayDatas!!.get(displayIndex) else null,
                    slot
                )
            )
        } else {
            displayList.set(displayIndex, null)
        }

        slot.displayList = displayList
    }

    /**
     * @private
     */
    protected abstract fun _buildTextureAtlasData(
        textureAtlasData: TextureAtlasData?,
        textureAtlas: Any?
    ): TextureAtlasData

    /**
     * @private
     */
    protected abstract fun _buildArmature(dataPackage: BuildArmaturePackage): Armature

    /**
     * @private
     */
    protected abstract fun _buildSlot(
        dataPackage: BuildArmaturePackage,
        slotData: SlotData,
        displays: Array<DisplayData>,
        armature: Armature
    ): Slot

    fun parseDragonBonesData(rawData: Any): DragonBonesData? {
        return parseDragonBonesData(rawData, null, 1f)
    }

    /**
     * 解析并添加龙骨数据。
     *
     * @param rawData 需要解析的原始数据。
     * @param name    为数据提供一个名称，以便可以通过这个名称获取数据，如果未设置，则使用数据中的名称。
     * @returns DragonBonesData
     * @version DragonBones 4.5
     * @language zh_CN
     * @see .getDragonBonesData
     * @see .addDragonBonesData
     * @see .removeDragonBonesData
     * @see DragonBonesData
     */
    fun parseDragonBonesData(rawData: Any, name: String?, scale: Float): DragonBonesData? {
        var dragonBonesData: DragonBonesData? = null
        if (rawData is ByteArray) {
            dragonBonesData = BaseFactory._binaryParser!!.parseDragonBonesData(rawData, scale)
        } else {
            dragonBonesData = this._dataParser!!.parseDragonBonesData(rawData, scale)
        }

        while (true) {
            val textureAtlasData = this._buildTextureAtlasData(null, null)
            if (this._dataParser!!.parseTextureAtlasData(null, textureAtlasData, scale)) {
                this.addTextureAtlasData(textureAtlasData, name)
            } else {
                textureAtlasData.returnToPool()
                break
            }
        }

        if (dragonBonesData != null) {
            this.addDragonBonesData(dragonBonesData, name)
        }

        return dragonBonesData
    }

    /**
     * 解析并添加贴图集数据。
     *
     * @param rawData      需要解析的原始数据。 (JSON)
     * @param textureAtlas 贴图。
     * @param name         为数据指定一个名称，以便可以通过这个名称获取数据，如果未设置，则使用数据中的名称。
     * @param scale        为贴图集设置一个缩放值。
     * @returns 贴图集数据
     * @version DragonBones 4.5
     * @language zh_CN
     * @see .getTextureAtlasData
     * @see .addTextureAtlasData
     * @see .removeTextureAtlasData
     * @see TextureAtlasData
     */
    @JvmOverloads
    fun parseTextureAtlasData(
        rawData: Any,
        textureAtlas: Any,
        name: String? = null,
        scale: Float = 0f
    ): TextureAtlasData {
        val textureAtlasData = this._buildTextureAtlasData(null, null)
        this._dataParser!!.parseTextureAtlasData(rawData, textureAtlasData, scale)
        this._buildTextureAtlasData(textureAtlasData, textureAtlas)
        this.addTextureAtlasData(textureAtlasData, name)

        return textureAtlasData
    }

    /**
     * @version DragonBones 5.1
     * @language zh_CN
     */
    fun updateTextureAtlasData(name: String, textureAtlases: Array<Any>) {
        val textureAtlasDatas = this.getTextureAtlasData(name)
        if (textureAtlasDatas != null) {
            var i = 0
            val l = textureAtlasDatas.size()
            while (i < l) {
                if (i < textureAtlases.size()) {
                    this._buildTextureAtlasData(textureAtlasDatas.get(i), textureAtlases.get(i))
                }
                ++i
            }
        }
    }

    /**
     * 获取指定名称的龙骨数据。
     *
     * @param name 数据名称。
     * @returns DragonBonesData
     * @version DragonBones 3.0
     * @language zh_CN
     * @see .parseDragonBonesData
     * @see .addDragonBonesData
     * @see .removeDragonBonesData
     * @see DragonBonesData
     */
    fun getDragonBonesData(name: String): DragonBonesData? {
        return this._dragonBonesDataMap[name]
    }

    /**
     * 添加龙骨数据。
     *
     * @param data 龙骨数据。
     * @param name 为数据指定一个名称，以便可以通过这个名称获取数据，如果未设置，则使用数据中的名称。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see .parseDragonBonesData
     * @see .getDragonBonesData
     * @see .removeDragonBonesData
     * @see DragonBonesData
     */
    @JvmOverloads
    fun addDragonBonesData(data: DragonBonesData, name: String? = null) {
        var name = name
        name = name ?: data.name
        if (this._dragonBonesDataMap.containsKey(name)) {
            if (this._dragonBonesDataMap[name] === data) {
                return
            }

            Console.warn("Replace data: " + name!!)
            this._dragonBonesDataMap[name].returnToPool()
        }

        this._dragonBonesDataMap[name] = data
    }

    /**
     * 移除龙骨数据。
     *
     * @param name        数据名称。
     * @param disposeData 是否释放数据。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see .parseDragonBonesData
     * @see .getDragonBonesData
     * @see .addDragonBonesData
     * @see DragonBonesData
     */
    @JvmOverloads
    fun removeDragonBonesData(name: String, disposeData: Boolean = true) {
        if (this._dragonBonesDataMap.containsKey(name)) {
            if (disposeData) {
                this._dragonBones!!.bufferObject(this._dragonBonesDataMap[name])
            }

            this._dragonBonesDataMap.remove(name)
        }
    }

    /**
     * 获取指定名称的贴图集数据列表。
     *
     * @param name 数据名称。
     * @returns 贴图集数据列表。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see .parseTextureAtlasData
     * @see .addTextureAtlasData
     * @see .removeTextureAtlasData
     * @see TextureAtlasData
     */
    fun getTextureAtlasData(name: String): Array<TextureAtlasData>? {
        return this._textureAtlasDataMap[name]
    }

    /**
     * 添加贴图集数据。
     *
     * @param data 贴图集数据。
     * @param name 为数据指定一个名称，以便可以通过这个名称获取数据，如果未设置，则使用数据中的名称。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see .parseTextureAtlasData
     * @see .getTextureAtlasData
     * @see .removeTextureAtlasData
     * @see TextureAtlasData
     */
    @JvmOverloads
    fun addTextureAtlasData(data: TextureAtlasData, name: String? = null) {
        var name = name
        name = name ?: data.name
        if (!this._textureAtlasDataMap.containsKey(name)) {
            this._textureAtlasDataMap[name] = Array()
        }
        val textureAtlasList = this._textureAtlasDataMap[name]

        if (textureAtlasList.indexOf(data) < 0) {
            textureAtlasList.add(data)
        }
    }

    /**
     * 移除贴图集数据。
     *
     * @param name        数据名称。
     * @param disposeData 是否释放数据。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see .parseTextureAtlasData
     * @see .getTextureAtlasData
     * @see .addTextureAtlasData
     * @see TextureAtlasData
     */
    @JvmOverloads
    fun removeTextureAtlasData(name: String, disposeData: Boolean = true) {
        if (this._textureAtlasDataMap.containsKey(name)) {
            val textureAtlasDataList = this._textureAtlasDataMap[name]
            if (disposeData) {
                for (textureAtlasData in textureAtlasDataList) {
                    this._dragonBones!!.bufferObject(textureAtlasData)
                }
            }

            this._textureAtlasDataMap.remove(name)
        }
    }

    /**
     * 获取骨架数据。
     *
     * @param name            骨架数据名称。
     * @param dragonBonesName 龙骨数据名称。
     * @version DragonBones 5.1
     * @language zh_CN
     * @see ArmatureData
     */
    @JvmOverloads
    fun getArmatureData(name: String, dragonBonesName: String = ""): ArmatureData? {
        val dataPackage = BuildArmaturePackage()
        return if (!this._fillBuildArmaturePackage(dataPackage, dragonBonesName, name, "", "")) {
            null
        } else dataPackage.armature

    }

    /**
     * 清除所有的数据。
     *
     * @param disposeData 是否释放数据。
     * @version DragonBones 4.5
     * @language zh_CN
     */
    @JvmOverloads
    fun clear(disposeData: Boolean = true) {
        for (k in this._dragonBonesDataMap.keys) {
            if (disposeData) {
                this._dragonBones!!.bufferObject(this._dragonBonesDataMap[k])
            }

            this._dragonBonesDataMap.remove(k)
        }

        for (k in this._textureAtlasDataMap.keys) {
            if (disposeData) {
                val textureAtlasDataList = this._textureAtlasDataMap[k]
                for (textureAtlasData in textureAtlasDataList) {
                    this._dragonBones!!.bufferObject(textureAtlasData)
                }
            }

            this._textureAtlasDataMap.remove(k)
        }
    }

    /**
     * 创建一个骨架。
     *
     * @param armatureName     骨架数据名称。
     * @param dragonBonesName  龙骨数据名称，如果未设置，将检索所有的龙骨数据，当多个龙骨数据中包含同名的骨架数据时，可能无法创建出准确的骨架。
     * @param skinName         皮肤名称，如果未设置，则使用默认皮肤。
     * @param textureAtlasName 贴图集数据名称，如果未设置，则使用龙骨数据名称。
     * @returns 骨架
     * @version DragonBones 3.0
     * @language zh_CN
     * @see ArmatureData
     *
     * @see Armature
     */
    @JvmOverloads
    fun buildArmature(
        armatureName: String,
        dragonBonesName: String? = null,
        skinName: String? = null,
        textureAtlasName: String? = null
    ): Armature? {
        val dataPackage = BuildArmaturePackage()
        if (!this._fillBuildArmaturePackage(
                dataPackage,
                dragonBonesName ?: "",
                armatureName,
                skinName ?: "",
                textureAtlasName ?: ""
            )
        ) {
            Console.warn("No armature data. " + armatureName + ", " + (dragonBonesName ?: ""))
            return null
        }

        val armature = this._buildArmature(dataPackage)
        this._buildBones(dataPackage, armature)
        this._buildSlots(dataPackage, armature)
        // armature.invalidUpdate(null, true); TODO
        armature.invalidUpdate("", true)
        armature.advanceTime(0f) // Update armature pose.

        return armature
    }

    /**
     * 用指定资源替换指定插槽的显示对象。(用 "dragonBonesName/armatureName/slotName/displayName" 的资源替换 "slot" 的显示对象)
     *
     * @param dragonBonesName 指定的龙骨数据名称。
     * @param armatureName    指定的骨架名称。
     * @param slotName        指定的插槽名称。
     * @param displayName     指定的显示对象名称。
     * @param slot            指定的插槽实例。
     * @param displayIndex    要替换的显示对象的索引，如果未设置，则替换当前正在显示的显示对象。
     * @version DragonBones 4.5
     * @language zh_CN
     */
    @JvmOverloads
    fun replaceSlotDisplay(
        dragonBonesName: String?,
        armatureName: String, slotName: String, displayName: String,
        slot: Slot, displayIndex: Int = -1
    ) {
        val dataPackage = BuildArmaturePackage()
        if (!this._fillBuildArmaturePackage(
                dataPackage,
                dragonBonesName ?: "",
                armatureName, "", ""
            ) || dataPackage.skin == null
        ) {
            return
        }

        val displays = dataPackage.skin!!.getDisplays(slotName) ?: return

        for (display in displays) {
            if (display != null && display.name == displayName) {
                this._replaceSlotDisplay(dataPackage, display, slot, displayIndex)
                break
            }
        }
    }

    /**
     * 用指定资源列表替换插槽的显示对象列表。
     *
     * @param dragonBonesName 指定的 DragonBonesData 名称。
     * @param armatureName    指定的骨架名称。
     * @param slotName        指定的插槽名称。
     * @param slot            指定的插槽实例。
     * @version DragonBones 4.5
     * @language zh_CN
     */
    fun replaceSlotDisplayList(
        dragonBonesName: String?,
        armatureName: String,
        slotName: String,
        slot: Slot
    ) {
        val dataPackage = BuildArmaturePackage()
        if (!this._fillBuildArmaturePackage(
                dataPackage,
                dragonBonesName ?: "",
                armatureName, "", ""
            ) || dataPackage.skin == null
        ) {
            return
        }

        val displays = dataPackage.skin!!.getDisplays(slotName) ?: return

        var displayIndex = 0
        for (displayData in displays) {
            this._replaceSlotDisplay(dataPackage, displayData, slot, displayIndex++)
        }
    }

    /**
     * 更换骨架皮肤。
     *
     * @param armature 骨架。
     * @param skin     皮肤数据。
     * @param exclude  不需要更新的插槽。
     * @version DragonBones 5.1
     * @language zh_CN
     * @see Armature
     *
     * @see SkinData
     */
    @JvmOverloads
    fun changeSkin(armature: Armature, skin: SkinData, exclude: Array<String>? = null) {
        for (slot in armature.slots) {
            if (!skin.displays.containsKey(slot.name) || exclude != null && exclude.indexOf(slot.name) >= 0) {
                continue
            }

            val displays = skin.displays[slot.name]
            val displayList = slot.displayList // Copy.
            displayList.length = displays.size() // Modify displayList length.
            run {
                var i = 0
                val l = displays.size()
                while (i < l) {
                    val displayData = displays.get(i)
                    if (displayData != null) {
                        displayList.set(i, this._getSlotDisplay(null, displayData, null, slot))
                    } else {
                        displayList.set(i, null)
                    }
                    ++i
                }
            }

            slot._rawDisplayDatas = displays
            slot._displayDatas.length = displays.size()
            var i = 0
            val l = slot._displayDatas.size()
            while (i < l) {
                slot._displayDatas.set(i, displays.get(i))
                ++i
            }

            slot.displayList = displayList
        }
    }

    /**
     * 将骨架的动画替换成其他骨架的动画。 (通常这些骨架应该具有相同的骨架结构)
     *
     * @param toArmature               指定的骨架。
     * @param fromArmatreName          其他骨架的名称。
     * @param fromSkinName             其他骨架的皮肤名称，如果未设置，则使用默认皮肤。
     * @param fromDragonBonesDataName  其他骨架属于的龙骨数据名称，如果未设置，则检索所有的龙骨数据。
     * @param replaceOriginalAnimation 是否替换原有的同名动画。
     * @returns 是否替换成功。
     * @version DragonBones 4.5
     * @language zh_CN
     * @see Armature
     *
     * @see ArmatureData
     */
    @JvmOverloads
    fun copyAnimationsToArmature(
        toArmature: Armature,
        fromArmatreName: String,
        fromSkinName: String? = null,
        fromDragonBonesDataName: String? = null,
        replaceOriginalAnimation: Boolean = true
    ): Boolean {
        val dataPackage = BuildArmaturePackage()
        if (this._fillBuildArmaturePackage(
                dataPackage,
                fromDragonBonesDataName ?: "",
                fromArmatreName,
                fromSkinName ?: "",
                ""
            )
        ) {
            val fromArmatureData = dataPackage.armature
            if (replaceOriginalAnimation) {
                toArmature.animation!!.animations = fromArmatureData!!.animations
            } else {
                val animations = HashMap<String, AnimationData>()

                for (animationName in toArmature.animation!!.animations.keys) {
                    animations[animationName] = toArmature.animation!!.animations[animationName]
                }

                for (animationName in fromArmatureData!!.animations.keys) {
                    animations[animationName] = fromArmatureData.animations[animationName]
                }

                toArmature.animation!!.animations = animations
            }

            if (dataPackage.skin != null) {
                val slots = toArmature.slots
                var i = 0
                val l = slots.size()
                while (i < l) {
                    val toSlot = slots.get(i)
                    val toSlotDisplayList = toSlot.displayList
                    var j = 0
                    val lJ = toSlotDisplayList.size()
                    while (j < lJ) {
                        val toDisplayObject = toSlotDisplayList.get(j)
                        if (toDisplayObject is Armature) {
                            val displays = dataPackage.skin!!.getDisplays(toSlot.name)
                            if (displays != null && j < displays.size()) {
                                val fromDisplayData = displays.get(j)
                                if (fromDisplayData != null && fromDisplayData.type == DisplayType.Armature) {
                                    this.copyAnimationsToArmature(
                                        toDisplayObject,
                                        fromDisplayData.path,
                                        fromSkinName,
                                        fromDragonBonesDataName,
                                        replaceOriginalAnimation
                                    )
                                }
                            }
                        }
                        ++j
                    }
                    ++i
                }

                return true
            }
        }

        return false
    }

    companion object {
        /**
         * @private
         */
        protected var _objectParser: ObjectDataParser? = null
        /**
         * @private
         */
        protected var _binaryParser: BinaryDataParser? = null
    }
}
