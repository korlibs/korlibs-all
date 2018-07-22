package com.dragonbones.model

import com.dragonbones.animation.AnimationState
import com.dragonbones.armature.Armature
import com.dragonbones.armature.Bone
import com.dragonbones.core.AnimationFadeOutMode
import com.dragonbones.core.BaseObject
import com.dragonbones.core.TweenType
import com.dragonbones.util.Array

/**
 * 动画配置，描述播放一个动画所需要的全部信息。
 *
 * @version DragonBones 5.0
 * @beta
 * @language zh_CN
 * @see AnimationState
 */
class AnimationConfig : BaseObject() {
    /**
     * 是否暂停淡出的动画。
     *
     * @default true
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var pauseFadeOut: Boolean = false
    /**
     * 淡出模式。
     *
     * @default dragonBones.AnimationFadeOutMode.All
     * @version DragonBones 5.0
     * @language zh_CN
     * @see AnimationFadeOutMode
     */
    var fadeOutMode: AnimationFadeOutMode
    /**
     * 淡出缓动方式。
     *
     * @default TweenType.Line
     * @version DragonBones 5.0
     * @language zh_CN
     * @see TweenType
     */
    var fadeOutTweenType: TweenType
    /**
     * 淡出时间。 [-1: 与淡入时间同步, [0~N]: 淡出时间] (以秒为单位)
     *
     * @default -1
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var fadeOutTime: Float = 0.toFloat()

    /**
     * 否能触发行为。
     *
     * @default true
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var actionEnabled: Boolean = false
    /**
     * 是否以增加的方式混合。
     *
     * @default false
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var additiveBlending: Boolean = false
    /**
     * 是否对插槽的显示对象有控制权。
     *
     * @default true
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var displayControl: Boolean = false
    /**
     * 是否暂停淡入的动画，直到淡入过程结束。
     *
     * @default true
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var pauseFadeIn: Boolean = false
    /**
     * 是否将没有动画的对象重置为初始值。
     *
     * @default true
     * @version DragonBones 5.1
     * @language zh_CN
     */
    var resetToPose: Boolean = false
    /**
     * 淡入缓动方式。
     *
     * @default TweenType.Line
     * @version DragonBones 5.0
     * @language zh_CN
     * @see TweenType
     */
    var fadeInTweenType: TweenType
    /**
     * 播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次]
     *
     * @default -1
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var playTimes: Int = 0
    /**
     * 混合图层，图层高会优先获取混合权重。
     *
     * @default 0
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var layer: Float = 0.toFloat()
    /**
     * 开始时间。 (以秒为单位)
     *
     * @default 0
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var position: Float = 0.toFloat()
    /**
     * 持续时间。 [-1: 使用动画数据默认值, 0: 动画停止, (0~N]: 持续时间] (以秒为单位)
     *
     * @default -1
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var duration: Float = 0.toFloat()
    /**
     * 播放速度。 [(-N~0): 倒转播放, 0: 停止播放, (0~1): 慢速播放, 1: 正常播放, (1~N): 快速播放]
     *
     * @default 1
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var timeScale: Float = 0.toFloat()
    /**
     * 淡入时间。 [-1: 使用动画数据默认值, [0~N]: 淡入时间] (以秒为单位)
     *
     * @default -1
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var fadeInTime: Float = 0.toFloat()
    /**
     * 自动淡出时间。 [-1: 不自动淡出, [0~N]: 淡出时间] (以秒为单位)
     *
     * @default -1
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var autoFadeOutTime: Float = 0.toFloat()
    /**
     * 混合权重。
     *
     * @default 1
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var weight: Float = 0.toFloat()
    /**
     * 动画状态名。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var name: String
    /**
     * 动画数据名。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var animation: String
    /**
     * 混合组，用于动画状态编组，方便控制淡出。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var group: String
    /**
     * 骨骼遮罩。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    val boneMask = Array<String>()

    /**
     * @private
     */
    override fun _onClear() {
        this.pauseFadeOut = true
        this.fadeOutMode = AnimationFadeOutMode.All
        this.fadeOutTweenType = TweenType.Line
        this.fadeOutTime = -1f

        this.actionEnabled = true
        this.additiveBlending = false
        this.displayControl = true
        this.pauseFadeIn = true
        this.resetToPose = true
        this.fadeInTweenType = TweenType.Line
        this.playTimes = -1
        this.layer = 0f
        this.position = 0f
        this.duration = -1f
        this.timeScale = -100f
        this.fadeInTime = -1f
        this.autoFadeOutTime = -1f
        this.weight = 1f
        this.name = ""
        this.animation = ""
        this.group = ""
        this.boneMask.clear()
    }

    fun clear() {
        this._onClear()
    }

    fun copyFrom(value: AnimationConfig) {
        this.pauseFadeOut = value.pauseFadeOut
        this.fadeOutMode = value.fadeOutMode
        this.autoFadeOutTime = value.autoFadeOutTime
        this.fadeOutTweenType = value.fadeOutTweenType

        this.actionEnabled = value.actionEnabled
        this.additiveBlending = value.additiveBlending
        this.displayControl = value.displayControl
        this.pauseFadeIn = value.pauseFadeIn
        this.resetToPose = value.resetToPose
        this.playTimes = value.playTimes
        this.layer = value.layer
        this.position = value.position
        this.duration = value.duration
        this.timeScale = value.timeScale
        this.fadeInTime = value.fadeInTime
        this.fadeOutTime = value.fadeOutTime
        this.fadeInTweenType = value.fadeInTweenType
        this.weight = value.weight
        this.name = value.name
        this.animation = value.animation
        this.group = value.group

        this.boneMask.length = value.boneMask.size()
        var i = 0
        val l = this.boneMask.size()
        while (i < l) {
            this.boneMask.set(i, value.boneMask.get(i))
            ++i
        }
    }

    fun containsBoneMask(name: String): Boolean {
        return this.boneMask.size() == 0 || this.boneMask.indexOfObject(name) >= 0
    }

    @JvmOverloads
    fun addBoneMask(armature: Armature, name: String, recursive: Boolean = true) {
        val currentBone = armature.getBone(name) ?: return

        if (this.boneMask.indexOfObject(name) < 0) { // Add mixing
            this.boneMask.add(name)
        }

        if (recursive) { // Add recursive mixing.
            for (bone in armature.bones) {
                if (this.boneMask.indexOfObject(bone.name) < 0 && currentBone.contains(bone)) {
                    this.boneMask.add(bone.name)
                }
            }
        }
    }

    @JvmOverloads
    fun removeBoneMask(armature: Armature, name: String, recursive: Boolean = true) {
        val index = this.boneMask.indexOfObject(name)
        if (index >= 0) { // Remove mixing.
            this.boneMask.splice(index, 1)
        }

        if (recursive) {
            val currentBone = armature.getBone(name)
            if (currentBone != null) {
                if (this.boneMask.size() > 0) { // Remove recursive mixing.
                    for (bone in armature.bones) {
                        val index2 = this.boneMask.indexOfObject(bone.name)
                        if (index2 >= 0 && currentBone.contains(bone)) {
                            this.boneMask.splice(index2, 1)
                        }
                    }
                } else { // Add unrecursive mixing.
                    for (bone in armature.bones) {
                        if (bone === currentBone) {
                            continue
                        }

                        if (!currentBone.contains(bone)) {
                            this.boneMask.add(bone.name)
                        }
                    }
                }
            }
        }
    }
}
