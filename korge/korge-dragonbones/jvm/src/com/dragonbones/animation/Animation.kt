package com.dragonbones.animation

import com.dragonbones.armature.Armature
import com.dragonbones.armature.Bone
import com.dragonbones.armature.Slot
import com.dragonbones.core.AnimationFadeOutMode
import com.dragonbones.core.BaseObject
import com.dragonbones.model.AnimationConfig
import com.dragonbones.model.AnimationData
import com.dragonbones.util.Array
import com.dragonbones.util.Console

import java.util.HashMap
import java.util.Objects

/**
 * 动画控制器，用来播放动画数据，管理动画状态。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see AnimationData
 *
 * @see AnimationState
 */
class Animation : BaseObject() {
    /**
     * 播放速度。 [0: 停止播放, (0~1): 慢速播放, 1: 正常播放, (1~N): 快速播放]
     *
     * @default 1f
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var timeScale: Float = 0.toFloat()

    private var _animationDirty: Boolean = false // Update bones and slots cachedFrameIndices.
    /**
     * @internal
     * @private
     */
    var _timelineDirty: Boolean = false // Updata animationStates timelineStates.
    /**
     * 所有动画数据名称。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     * @see .getAnimations
     */
    val animationNames = Array<String>()
    /**
     * 获取所有的动画状态。
     *
     * @version DragonBones 5.1
     * @language zh_CN
     * @see AnimationState
     */
    val states = Array<AnimationState>()
    private val _animations = HashMap<String, AnimationData>()
    private var _armature: Armature? = null
    private var _animationConfig: AnimationConfig? = null // Initial value.
    /**
     * 上一个正在播放的动画状态。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see AnimationState
     */
    var lastAnimationState: AnimationState? = null
        private set

    /**
     * 动画是否处于播放状态。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    val isPlaying: Boolean
        get() {
            for (animationState in this.states) {
                if (animationState.isPlaying) {
                    return true
                }
            }

            return false
        }

    /**
     * 所有动画状态是否均已播放完毕。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see AnimationState
     */
    val isCompleted: Boolean
        get() {
            for (animationState in this.states) {
                if (!animationState.isCompleted) {
                    return false
                }
            }

            return this.states.size() > 0
        }

    /**
     * 上一个正在播放的动画状态名称。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see .getLastAnimationState
     */
    val lastAnimationName: String
        get() = if (this.lastAnimationState != null) this.lastAnimationState!!.name else ""

    /**
     * 所有动画数据。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationData
     */
    var animations: Map<String, AnimationData>
        get() = this._animations
        set(value) {
            if (this._animations === value) {
                return
            }

            this.animationNames.clear()
            this._animations.clear()

            for (k in value.keys) {
                this._animations[k] = value[k]!!
                this.animationNames.add(k)
            }
        }

    /**
     * 一个可以快速使用的动画配置实例。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     * @see AnimationConfig
     */
    val animationConfig: AnimationConfig
        get() {
            this._animationConfig!!.clear()
            return this._animationConfig!!
        }

    /**
     * @see .getAnimationNames
     * @see .getAnimations
     */
    val animationDataList: Array<AnimationData>
        @Deprecated("已废弃，请参考 @see")
        get() {
            val list = Array<AnimationData>()
            var i = 0
            val l = this.animationNames.size()
            while (i < l) {
                list.push(this._animations[this.animationNames.get(i)]!!)
                ++i
            }

            return list
        }

    /**
     * @private
     */
    override fun _onClear() {
        for (animationState in this.states) {
            animationState.returnToPool()
        }

        for (k in this._animations.keys) {
            this._animations.remove(k)
        }

        if (this._animationConfig != null) {
            this._animationConfig!!.returnToPool()
        }

        this.timeScale = 1f

        this._animationDirty = false
        this._timelineDirty = false
        this.animationNames.clear()
        this.states.clear()
        //this._animations.clear();
        this._armature = null //
        this._animationConfig = null //
        this.lastAnimationState = null
    }

    private fun _fadeOut(animationConfig: AnimationConfig) {
        when (animationConfig.fadeOutMode) {
            AnimationFadeOutMode.SameLayer -> for (animationState in this.states) {
                if (animationState.layer == animationConfig.layer) {
                    animationState.fadeOut(animationConfig.fadeOutTime, animationConfig.pauseFadeOut)
                }
            }

            AnimationFadeOutMode.SameGroup -> for (animationState in this.states) {
                if (animationState.group == animationConfig.group) {
                    animationState.fadeOut(animationConfig.fadeOutTime, animationConfig.pauseFadeOut)
                }
            }

            AnimationFadeOutMode.SameLayerAndGroup -> for (animationState in this.states) {
                if (animationState.layer == animationConfig.layer && animationState.group == animationConfig.group) {
                    animationState.fadeOut(animationConfig.fadeOutTime, animationConfig.pauseFadeOut)
                }
            }

            AnimationFadeOutMode.All -> for (animationState in this.states) {
                animationState.fadeOut(animationConfig.fadeOutTime, animationConfig.pauseFadeOut)
            }

            AnimationFadeOutMode.None, AnimationFadeOutMode.Single -> {
            }
            else -> {
            }
        }
    }

    /**
     * @internal
     * @private
     */
    fun init(armature: Armature) {
        if (this._armature != null) {
            return
        }

        this._armature = armature
        this._animationConfig = BaseObject.borrowObject(AnimationConfig::class.java)
    }

    /**
     * @internal
     * @private
     */
    fun advanceTime(passedTime: Float) {
        var passedTime = passedTime
        if (passedTime < 0f) { // Only animationState can reverse play.
            passedTime = -passedTime
        }

        if (this._armature!!.inheritAnimation && this._armature!!.parent != null) { // Inherit parent animation timeScale.
            passedTime *= this._armature!!.parent!!.armature!!.animation!!.timeScale
        }

        if (this.timeScale != 1f) {
            passedTime *= this.timeScale
        }

        val animationStateCount = this.states.size()
        if (animationStateCount == 1) {
            val animationState = this.states.get(0)
            if (animationState._fadeState > 0 && animationState._subFadeState > 0) {
                this._armature!!._dragonBones!!.bufferObject(animationState)
                this.states.clear()
                this.lastAnimationState = null
            } else {
                val animationData = animationState.clip
                val cacheFrameRate = animationData!!.cacheFrameRate
                if (this._animationDirty && cacheFrameRate > 0f) { // Update cachedFrameIndices.
                    this._animationDirty = false
                    for (bone in this._armature!!.bones) {
                        bone._cachedFrameIndices = animationData.getBoneCachedFrameIndices(bone.name)
                    }

                    for (slot in this._armature!!.slots) {
                        slot._cachedFrameIndices = animationData.getSlotCachedFrameIndices(slot.name)
                    }
                }

                if (this._timelineDirty) {
                    animationState.updateTimelines()
                }

                animationState.advanceTime(passedTime, cacheFrameRate)
            }
        } else if (animationStateCount > 1) {
            var i = 0
            var r = 0
            while (i < animationStateCount) {
                val animationState = this.states.get(i)
                if (animationState._fadeState > 0 && animationState._subFadeState > 0) {
                    r++
                    this._armature!!._dragonBones!!.bufferObject(animationState)
                    this._animationDirty = true
                    if (this.lastAnimationState === animationState) { // Update last animation state.
                        this.lastAnimationState = null
                    }
                } else {
                    if (r > 0) {
                        this.states.set(i - r, animationState)
                    }

                    if (this._timelineDirty) {
                        animationState.updateTimelines()
                    }

                    animationState.advanceTime(passedTime, 0f)
                }

                if (i == animationStateCount - 1 && r > 0) { // Modify animation states size.
                    this.states.length = this.states.size() - r
                    if (this.lastAnimationState == null && this.states.size() > 0) {
                        this.lastAnimationState = this.states.get(this.states.size() - 1)
                    }
                }
                ++i
            }

            this._armature!!._cacheFrameIndex = -1
        } else {
            this._armature!!._cacheFrameIndex = -1
        }

        this._timelineDirty = false
    }

    /**
     * 清除所有动画状态。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationState
     */
    fun reset() {
        for (animationState in this.states) {
            animationState.returnToPool()
        }

        this._animationDirty = false
        this._timelineDirty = false
        this._animationConfig!!.clear()
        this.states.clear()
        this.lastAnimationState = null
    }

    /**
     * 暂停播放动画。
     *
     * @param animationName 动画状态的名称，如果未设置，则暂停所有动画状态。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see AnimationState
     */
    @JvmOverloads
    fun stop(animationName: String? = null) {
        if (animationName != null) {
            val animationState = this.getState(animationName)
            animationState?.stop()
        } else {
            for (animationState in this.states) {
                animationState.stop()
            }
        }
    }

    /**
     * 通过动画配置来播放动画。
     *
     * @param animationConfig 动画配置。
     * @returns 对应的动画状态。
     * @version DragonBones 5.0
     * @beta
     * @language zh_CN
     * @see AnimationConfig
     *
     * @see AnimationState
     */
    fun playConfig(animationConfig: AnimationConfig?): AnimationState? {
        val animationName = animationConfig!!.animation
        if (!this._animations.containsKey(animationName)) {
            Console.warn(
                "Non-existent animation.\n" +
                        "DragonBones name: " + this._armature!!.armatureData!!.parent!!.name +
                        "Armature name: " + this._armature!!.name +
                        "Animation name: " + animationName
            )

            return null
        }

        val animationData = this._animations[animationName]!!

        if (animationConfig.fadeOutMode == AnimationFadeOutMode.Single) {
            for (animationState in this.states) {
                if (animationState.clip === animationData) {
                    return animationState
                }
            }
        }

        if (this.states.size() == 0) {
            animationConfig.fadeInTime = 0f
        } else if (animationConfig.fadeInTime < 0f) {
            animationConfig.fadeInTime = animationData.fadeInTime
        }

        if (animationConfig.fadeOutTime < 0f) {
            animationConfig.fadeOutTime = animationConfig.fadeInTime
        }

        if (animationConfig.timeScale <= -100.0) {
            animationConfig.timeScale = 1f / animationData.scale
        }

        if (animationData.frameCount > 1) {
            if (animationConfig.position < 0f) {
                animationConfig.position %= animationData.duration
                animationConfig.position = animationData.duration - animationConfig.position
            } else if (animationConfig.position == animationData.duration) {
                animationConfig.position -= 0.000001f // Play a little time before end.
            } else if (animationConfig.position > animationData.duration) {
                animationConfig.position %= animationData.duration
            }

            if (animationConfig.duration > 0f && animationConfig.position + animationConfig.duration > animationData.duration) {
                animationConfig.duration = animationData.duration - animationConfig.position
            }

            if (animationConfig.playTimes < 0) {
                animationConfig.playTimes = animationData.playTimes
            }
        } else {
            animationConfig.playTimes = 1
            animationConfig.position = 0f
            if (animationConfig.duration > 0f) {
                animationConfig.duration = 0f
            }
        }

        if (animationConfig.duration == 0f) {
            animationConfig.duration = -1f
        }

        this._fadeOut(animationConfig)

        val animationState = BaseObject.borrowObject(AnimationState::class.java)
        animationState.init(this._armature!!, animationData!!, animationConfig)
        this._animationDirty = true
        this._armature!!._cacheFrameIndex = -1

        if (this.states.size() > 0) {
            var added = false
            var i = 0
            val l = this.states.size()
            while (i < l) {
                if (animationState.layer >= this.states.get(i).layer) {
                } else {
                    added = true
                    this.states.splice(i + 1, 0, animationState)
                    break
                }
                ++i
            }

            if (!added) {
                this.states.add(animationState)
            }
        } else {
            this.states.add(animationState)
        }

        // Child armature play same name animation.
        for (slot in this._armature!!.slots) {
            val childArmature = slot.childArmature
            if (childArmature != null && childArmature.inheritAnimation &&
                childArmature.animation!!.hasAnimation(animationName) &&
                childArmature.animation!!.getState(animationName) == null
            ) {
                childArmature.animation!!.fadeIn(animationName) //
            }
        }

        if (animationConfig.fadeInTime <= 0f) { // Blend animation state, update armature.
            this._armature!!.advanceTime(0f)
        }

        this.lastAnimationState = animationState

        return animationState
    }

    /**
     * 播放动画。
     *
     * @param animationName 动画数据名称，如果未设置，则播放默认动画，或将暂停状态切换为播放状态，或重新播放上一个正在播放的动画。
     * @param playTimes     播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次]
     * @returns 对应的动画状态。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see AnimationState
     */
    @JvmOverloads
    fun play(animationName: String? = null, playTimes: Int = -1): AnimationState? {
        this._animationConfig!!.clear()
        this._animationConfig!!.resetToPose = true
        this._animationConfig!!.playTimes = playTimes
        this._animationConfig!!.fadeInTime = 0f
        this._animationConfig!!.animation = animationName ?: ""

        if (animationName != null && animationName.length > 0) {
            this.playConfig(this._animationConfig)
        } else if (this.lastAnimationState == null) {
            val defaultAnimation = this._armature!!.armatureData!!.defaultAnimation
            if (defaultAnimation != null) {
                this._animationConfig!!.animation = defaultAnimation.name
                this.playConfig(this._animationConfig)
            }
        } else if (!this.lastAnimationState!!.isPlaying && !this.lastAnimationState!!.isCompleted) {
            this.lastAnimationState!!.play()
        } else {
            this._animationConfig!!.animation = this.lastAnimationState!!.name
            this.playConfig(this._animationConfig)
        }

        return this.lastAnimationState
    }

    /**
     * 淡入播放动画。
     *
     * @param animationName 动画数据名称。
     * @param playTimes     播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次]
     * @param fadeInTime    淡入时间。 [-1: 使用动画数据默认值, [0~N]: 淡入时间] (以秒为单位)
     * @param layer         混合图层，图层高会优先获取混合权重。
     * @param group         混合组，用于动画状态编组，方便控制淡出。
     * @param fadeOutMode   淡出模式。
     * @returns 对应的动画状态。
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationFadeOutMode
     *
     * @see AnimationState
     */
    @JvmOverloads
    fun fadeIn(
        animationName: String,
        fadeInTime: Float = -1f,
        playTimes: Int = -1,
        layer: Int = 0,
        group: String? = null,
        fadeOutMode: AnimationFadeOutMode = AnimationFadeOutMode.SameLayerAndGroup
    ): AnimationState? {
        this._animationConfig!!.clear()
        this._animationConfig!!.fadeOutMode = fadeOutMode
        this._animationConfig!!.playTimes = playTimes
        this._animationConfig!!.layer = layer.toFloat()
        this._animationConfig!!.fadeInTime = fadeInTime
        this._animationConfig!!.animation = animationName
        this._animationConfig!!.group = group ?: ""

        return this.playConfig(this._animationConfig)
    }

    /**
     * 从指定时间开始播放动画。
     *
     * @param animationName 动画数据的名称。
     * @param time          开始时间。 (以秒为单位)
     * @param playTimes     播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次]
     * @returns 对应的动画状态。
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationState
     */
    @JvmOverloads
    fun gotoAndPlayByTime(animationName: String, time: Float = 0f, playTimes: Int = -1): AnimationState? {
        this._animationConfig!!.clear()
        this._animationConfig!!.resetToPose = true
        this._animationConfig!!.playTimes = playTimes
        this._animationConfig!!.position = time
        this._animationConfig!!.fadeInTime = 0f
        this._animationConfig!!.animation = animationName

        return this.playConfig(this._animationConfig)
    }

    /**
     * 从指定帧开始播放动画。
     *
     * @param animationName 动画数据的名称。
     * @param frame         帧。
     * @param playTimes     播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次]
     * @returns 对应的动画状态。
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationState
     */
    @JvmOverloads
    fun gotoAndPlayByFrame(animationName: String, frame: Int = 0, playTimes: Int = -1): AnimationState? {
        this._animationConfig!!.clear()
        this._animationConfig!!.resetToPose = true
        this._animationConfig!!.playTimes = playTimes
        this._animationConfig!!.fadeInTime = 0f
        this._animationConfig!!.animation = animationName

        val animationData = this._animations[animationName]
        if (animationData != null) {
            this._animationConfig!!.position = animationData.duration * frame / animationData.frameCount
        }

        return this.playConfig(this._animationConfig)
    }

    /**
     * 从指定进度开始播放动画。
     *
     * @param animationName 动画数据的名称。
     * @param progress      进度。 [0~1]
     * @param playTimes     播放次数。 [-1: 使用动画数据默认值, 0: 无限循环播放, [1~N]: 循环播放 N 次]
     * @returns 对应的动画状态。
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationState
     */
    @JvmOverloads
    fun gotoAndPlayByProgress(animationName: String, progress: Float = 0f, playTimes: Int = -1): AnimationState? {
        this._animationConfig!!.clear()
        this._animationConfig!!.resetToPose = true
        this._animationConfig!!.playTimes = playTimes
        this._animationConfig!!.fadeInTime = 0f
        this._animationConfig!!.animation = animationName

        val animationData = this._animations[animationName]
        if (animationData != null) {
            this._animationConfig!!.position = animationData.duration * if (progress > 0f) progress else 0f
        }

        return this.playConfig(this._animationConfig)
    }

    fun gotoAndStopByTime(animationName: String): AnimationState? {
        return gotoAndStopByTime(animationName, 0f)
    }

    /**
     * 将动画停止到指定的时间。
     *
     * @param animationName 动画数据的名称。
     * @param time          时间。 (以秒为单位)
     * @returns 对应的动画状态。
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationState
     */
    fun gotoAndStopByTime(animationName: String, time: Float): AnimationState? {
        val animationState = this.gotoAndPlayByTime(animationName, time, 1)
        animationState?.stop()

        return animationState
    }

    /**
     * 将动画停止到指定的帧。
     *
     * @param animationName 动画数据的名称。
     * @param frame         帧。
     * @returns 对应的动画状态。
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationState
     */
    @JvmOverloads
    fun gotoAndStopByFrame(animationName: String, frame: Int = 0): AnimationState? {
        val animationState = this.gotoAndPlayByFrame(animationName, frame, 1)
        animationState?.stop()

        return animationState
    }

    /**
     * 将动画停止到指定的进度。
     *
     * @param animationName 动画数据的名称。
     * @param progress      进度。 [0 ~ 1]
     * @returns 对应的动画状态。
     * @version DragonBones 4.5
     * @language zh_CN
     * @see AnimationState
     */
    @JvmOverloads
    fun gotoAndStopByProgress(animationName: String, progress: Float = 0f): AnimationState? {
        val animationState = this.gotoAndPlayByProgress(animationName, progress, 1)
        animationState?.stop()

        return animationState
    }

    /**
     * 获取动画状态。
     *
     * @param animationName 动画状态的名称。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see AnimationState
     */
    fun getState(animationName: String): AnimationState? {
        var i = this.states.size()
        while (i-- != 0) {
            val animationState = this.states.get(i)
            if (animationState.name == animationName) {
                return animationState
            }
        }

        return null
    }

    /**
     * 是否包含动画数据。
     *
     * @param animationName 动画数据的名称。
     * @version DragonBones 3.0
     * @language zh_CN
     * @see AnimationData
     */
    fun hasAnimation(animationName: String): Boolean {
        return this._animations.containsKey(animationName)
    }

    fun gotoAndPlay(animationName: String): AnimationState? {
        return gotoAndPlay(animationName, -1f, -1f, -1, 0, null, AnimationFadeOutMode.SameLayerAndGroup, true, true)
    }

    /**
     * @see .play
     * @see .fadeIn
     * @see .gotoAndPlayByTime
     * @see .gotoAndPlayByFrame
     * @see .gotoAndPlayByProgress
     */
    @Deprecated("已废弃，请参考 @see")
    fun gotoAndPlay(
        animationName: String, fadeInTime: Float, duration: Float, playTimes: Int,
        layer: Int, group: String?, fadeOutMode: AnimationFadeOutMode,
        pauseFadeOut: Boolean, pauseFadeIn: Boolean
    ): AnimationState? {
        //pauseFadeOut;
        //pauseFadeIn;
        this._animationConfig!!.clear()
        this._animationConfig!!.resetToPose = true
        this._animationConfig!!.fadeOutMode = fadeOutMode
        this._animationConfig!!.playTimes = playTimes
        this._animationConfig!!.layer = layer.toFloat()
        this._animationConfig!!.fadeInTime = fadeInTime
        this._animationConfig!!.animation = animationName
        this._animationConfig!!.group = group ?: ""

        val animationData = this._animations[animationName]
        if (animationData != null && duration > 0f) {
            this._animationConfig!!.timeScale = animationData.duration / duration
        }

        return this.playConfig(this._animationConfig)
    }

    /**
     * @see .gotoAndStopByTime
     * @see .gotoAndStopByFrame
     * @see .gotoAndStopByProgress
     */
    @Deprecated("已废弃，请参考 @see")
    @JvmOverloads
    fun gotoAndStop(animationName: String, time: Float = 0f): AnimationState? {
        return this.gotoAndStopByTime(animationName, time)
    }

    /**
     * @see .getAnimationNames
     * @see .getAnimations
     */
    @Deprecated("已废弃，请参考 @see")
    fun getAnimationList(): Array<String> {
        return this.animationNames
    }
}
