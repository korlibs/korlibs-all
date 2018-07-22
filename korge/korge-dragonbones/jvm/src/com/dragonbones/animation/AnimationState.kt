package com.dragonbones.animation

import com.dragonbones.armature.Armature
import com.dragonbones.armature.Bone
import com.dragonbones.armature.Slot
import com.dragonbones.core.*
import com.dragonbones.event.EventObject
import com.dragonbones.model.*
import com.dragonbones.util.Array
import com.dragonbones.util.IntArray

import java.util.HashMap

/**
 * 动画状态，播放动画时产生，可以对每个播放的动画进行更细致的控制和调节。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 * @see Animation
 *
 * @see AnimationData
 */
class AnimationState : BaseObject() {
    /**
     * 是否将骨架的骨骼和插槽重置为绑定姿势（如果骨骼和插槽在这个动画状态中没有动画）。
     *
     * @version DragonBones 5.1
     * @language zh_CN
     */
    var resetToPose: Boolean = false
    /**
     * 是否以增加的方式混合。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var additiveBlending: Boolean = false
    /**
     * 是否对插槽的显示对象有控制权。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     * @see Slot.displayController
     */
    var displayControl: Boolean = false
    /**
     * 是否能触发行为。
     *
     * @version DragonBones 5.0
     * @language zh_CN
     */
    var actionEnabled: Boolean = false
    /**
     * 混合图层。
     *
     * @version DragonBones 3.0
     * @readonly
     * @language zh_CN
     */
    var layer: Float = 0.toFloat()
    /**
     * 播放次数。 [0: 无限循环播放, [1~N]: 循环播放 N 次]
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var playTimes: Int = 0
    /**
     * 播放速度。 [(-N~0): 倒转播放, 0: 停止播放, (0~1): 慢速播放, 1: 正常播放, (1~N): 快速播放]
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var timeScale: Float = 0.toFloat()
    /**
     * 混合权重。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var weight: Float = 0.toFloat()
    /**
     * 自动淡出时间。 [-1: 不自动淡出, [0~N]: 淡出时间] (以秒为单位)
     * 当设置一个大于等于 0 的值，动画状态将会在播放完成后自动淡出。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var autoFadeOutTime: Float = 0.toFloat()
    /**
     * @private
     */
    var fadeTotalTime: Float = 0.toFloat()
    /**
     * 动画名称。
     *
     * @version DragonBones 3.0
     * @readonly
     * @language zh_CN
     */
    var name: String? = null
    /**
     * 混合组。
     *
     * @version DragonBones 3.0
     * @readonly
     * @language zh_CN
     */
    var group: String? = null
    /**
     * 动画数据。
     *
     * @version DragonBones 3.0
     * @readonly
     * @language zh_CN
     * @see AnimationData
     */
    /**
     * @see .animationData
     *
     */
    @get:Deprecated("已废弃，请参考 @see")
    var clip: AnimationData? = null

    private var _timelineDirty: Boolean = false
    /**
     * @internal
     * @private xx: Play Enabled, Fade Play Enabled
     */
    var _playheadState: Int = 0
    /**
     * @internal
     * @private -1: Fade in, 0: Fade complete, 1: Fade out;
     */
    var _fadeState: Float = 0.toFloat()
    /**
     * @internal
     * @private -1: Fade start, 0: Fading, 1: Fade complete;
     */
    var _subFadeState: Float = 0.toFloat()
    /**
     * @internal
     * @private
     */
    var _position: Float = 0.toFloat()
    /**
     * @internal
     * @private
     */
    /**
     * 总时间。 (以秒为单位)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var totalTime: Float = 0.toFloat()
    private var _fadeTime: Float = 0.toFloat()
    private var _time: Float = 0.toFloat()
    /**
     * @internal
     * @private
     */
    var _fadeProgress: Float = 0.toFloat()
    private var _weightResult: Float = 0.toFloat()
    private val _boneMask = Array<String>()
    private val _boneTimelines = Array<BoneTimelineState>()
    private val _slotTimelines = Array<SlotTimelineState>()
    private val _bonePoses = HashMap<String, BonePose>()
    private var _armature: Armature? = null
    /**
     * @internal
     * @private
     */
    var _actionTimeline: ActionTimelineState? = null // Initial value.
    private var _zOrderTimeline: ZOrderTimelineState? = null // Initial value.

    /**
     * 是否正在淡入。
     *
     * @version DragonBones 5.1
     * @language zh_CN
     */
    val isFadeIn: Boolean
        get() = this._fadeState < 0

    /**
     * 是否正在淡出。
     *
     * @version DragonBones 5.1
     * @language zh_CN
     */
    val isFadeOut: Boolean
        get() = this._fadeState > 0

    /**
     * 是否淡入完毕。
     *
     * @version DragonBones 5.1
     * @language zh_CN
     */
    val isFadeComplete: Boolean
        get() = this._fadeState == 0f

    /**
     * 是否正在播放。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    val isPlaying: Boolean
        get() = this._playheadState and 2 != 0 && this._actionTimeline!!.playState <= 0

    /**
     * 是否播放完毕。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    val isCompleted: Boolean
        get() = this._actionTimeline!!.playState > 0

    /**
     * 当前播放次数。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    val currentPlayTimes: Int
        get() = this._actionTimeline!!.currentPlayTimes

    /**
     * 当前播放的时间。 (以秒为单位)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var currentTime: Float
        get() = this._actionTimeline!!.mcurrentTime
        set(value) {
            var value = value
            val currentPlayTimes =
                this._actionTimeline!!.currentPlayTimes - if (this._actionTimeline!!.playState > 0) 1 else 0
            if (value < 0 || this.totalTime < value) {
                value = value % this.totalTime + currentPlayTimes * this.totalTime
                if (value < 0) {
                    value += this.totalTime
                }
            }

            if (this.playTimes > 0 && currentPlayTimes == this.playTimes - 1 && value == this.totalTime) {
                value = this.totalTime - 0.000001f
            }

            if (this._time == value) {
                return
            }

            this._time = value
            this._actionTimeline!!.setCurrentTime(this._time)

            if (this._zOrderTimeline != null) {
                this._zOrderTimeline!!.playState = -1
            }

            for (timeline in this._boneTimelines) {
                timeline.playState = -1
            }

            for (timeline in this._slotTimelines) {
                timeline.playState = -1
            }
        }

    /**
     * @private
     */
    override fun _onClear() {
        for (timeline in this._boneTimelines) {
            timeline.returnToPool()
        }

        for (timeline in this._slotTimelines) {
            timeline.returnToPool()
        }

        for (k in this._bonePoses.keys) {
            this._bonePoses[k]?.returnToPool()
            this._bonePoses.remove(k)
        }

        if (this._actionTimeline != null) {
            this._actionTimeline!!.returnToPool()
        }

        if (this._zOrderTimeline != null) {
            this._zOrderTimeline!!.returnToPool()
        }

        this.resetToPose = false
        this.additiveBlending = false
        this.displayControl = false
        this.actionEnabled = false
        this.layer = 0f
        this.playTimes = 1
        this.timeScale = 1f
        this.weight = 1f
        this.autoFadeOutTime = 0f
        this.fadeTotalTime = 0f
        this.name = ""
        this.group = ""
        this.clip = null //

        this._timelineDirty = true
        this._playheadState = 0
        this._fadeState = -1f
        this._subFadeState = -1f
        this._position = 0f
        this.totalTime = 0f
        this._fadeTime = 0f
        this._time = 0f
        this._fadeProgress = 0f
        this._weightResult = 0f
        this._boneMask.clear()
        this._boneTimelines.clear()
        this._slotTimelines.clear()
        // this._bonePoses.clear();
        this._armature = null //
        this._actionTimeline = null //
        this._zOrderTimeline = null
    }

    private fun _isDisabled(slot: Slot?): Boolean {
        if (this.displayControl) {
            val displayController = slot!!.displayController
            if (displayController == null ||
                displayController == this.name ||
                displayController == this.group
            ) {
                return false
            }
        }

        return true
    }

    private fun _advanceFadeTime(passedTime: Float) {
        var passedTime = passedTime
        val isFadeOut = this._fadeState > 0

        if (this._subFadeState < 0) { // Fade start event.
            this._subFadeState = 0f

            val eventType = if (isFadeOut) EventObject.FADE_OUT else EventObject.FADE_IN
            if (this._armature!!.eventDispatcher!!.hasEvent(eventType)) {
                val eventObject = BaseObject.borrowObject(EventObject::class.java)
                eventObject.type = eventType
                eventObject.armature = this._armature
                eventObject.animationState = this
                this._armature!!._dragonBones!!.bufferEvent(eventObject)
            }
        }

        if (passedTime < 0f) {
            passedTime = -passedTime
        }

        this._fadeTime += passedTime

        if (this._fadeTime >= this.fadeTotalTime) { // Fade complete.
            this._subFadeState = 1f
            this._fadeProgress = if (isFadeOut) 0f else 1f
        } else if (this._fadeTime > 0f) { // Fading.
            this._fadeProgress =
                    if (isFadeOut) 1f - this._fadeTime / this.fadeTotalTime else this._fadeTime / this.fadeTotalTime
        } else { // Before fade.
            this._fadeProgress = if (isFadeOut) 1f else 0f
        }

        if (this._subFadeState > 0) { // Fade complete event.
            if (!isFadeOut) {
                this._playheadState = this._playheadState or 1 // x1
                this._fadeState = 0f
            }

            val eventType = if (isFadeOut) EventObject.FADE_OUT_COMPLETE else EventObject.FADE_IN_COMPLETE
            if (this._armature!!.eventDispatcher!!.hasEvent(eventType)) {
                val eventObject = BaseObject.borrowObject(EventObject::class.java)
                eventObject.type = eventType
                eventObject.armature = this._armature
                eventObject.animationState = this
                this._armature!!._dragonBones!!.bufferEvent(eventObject)
            }
        }
    }

    private fun _blendBoneTimline(timeline: BoneTimelineState) {
        val bone = timeline.bone
        val bonePose = timeline.bonePose!!.result
        val animationPose = bone!!.animationPose
        var boneWeight = if (this._weightResult > 0f) this._weightResult else -this._weightResult

        if (!bone._blendDirty) {
            bone._blendDirty = true
            bone._blendLayer = this.layer
            bone._blendLayerWeight = boneWeight
            bone._blendLeftWeight = 1f

            animationPose.x = bonePose.x * boneWeight
            animationPose.y = bonePose.y * boneWeight
            animationPose.rotation = bonePose.rotation * boneWeight
            animationPose.skew = bonePose.skew * boneWeight
            animationPose.scaleX = (bonePose.scaleX - 1f) * boneWeight + 1f
            animationPose.scaleY = (bonePose.scaleY - 1f) * boneWeight + 1f
        } else {
            boneWeight *= bone._blendLeftWeight
            bone._blendLayerWeight += boneWeight

            animationPose.x += bonePose.x * boneWeight
            animationPose.y += bonePose.y * boneWeight
            animationPose.rotation += bonePose.rotation * boneWeight
            animationPose.skew += bonePose.skew * boneWeight
            animationPose.scaleX += (bonePose.scaleX - 1f) * boneWeight
            animationPose.scaleY += (bonePose.scaleY - 1f) * boneWeight
        }

        if (this._fadeState != 0f || this._subFadeState != 0f) {
            bone._transformDirty = true
        }
    }

    /**
     * @private
     * @internal
     */
    fun init(armature: Armature, animationData: AnimationData, animationConfig: AnimationConfig) {
        if (this._armature != null) {
            return
        }

        this._armature = armature

        this.clip = animationData
        this.resetToPose = animationConfig.resetToPose
        this.additiveBlending = animationConfig.additiveBlending
        this.displayControl = animationConfig.displayControl
        this.actionEnabled = animationConfig.actionEnabled
        this.layer = animationConfig.layer
        this.playTimes = animationConfig.playTimes
        this.timeScale = animationConfig.timeScale
        this.fadeTotalTime = animationConfig.fadeInTime
        this.autoFadeOutTime = animationConfig.autoFadeOutTime
        this.weight = animationConfig.weight
        this.name = if (animationConfig.name.length > 0) animationConfig.name else animationConfig.animation
        this.group = animationConfig.group

        if (animationConfig.pauseFadeIn) {
            this._playheadState = 2 // 10
        } else {
            this._playheadState = 3 // 11
        }

        if (animationConfig.duration < 0f) {
            this._position = 0f
            this.totalTime = this.clip!!.duration
            if (animationConfig.position != 0f) {
                if (this.timeScale >= 0f) {
                    this._time = animationConfig.position
                } else {
                    this._time = animationConfig.position - this.totalTime
                }
            } else {
                this._time = 0f
            }
        } else {
            this._position = animationConfig.position
            this.totalTime = animationConfig.duration
            this._time = 0f
        }

        if (this.timeScale < 0f && this._time == 0f) {
            this._time = -0.000001f // Turn to end.
        }

        if (this.fadeTotalTime <= 0f) {
            this._fadeProgress = 0.999999f // Make different.
        }

        if (animationConfig.boneMask.size() > 0) {
            this._boneMask.length = animationConfig.boneMask.size()
            var i = 0
            val l = this._boneMask.size()
            while (i < l) {
                this._boneMask.set(i, animationConfig.boneMask.get(i))
                ++i
            }
        }

        this._actionTimeline = BaseObject.borrowObject(ActionTimelineState::class.java)
        this._actionTimeline!!.init(this._armature!!, this, this.clip!!.actionTimeline)
        this._actionTimeline!!.mcurrentTime = this._time
        if (this._actionTimeline!!.mcurrentTime < 0f) {
            this._actionTimeline!!.mcurrentTime = this.totalTime - this._actionTimeline!!.mcurrentTime
        }

        if (this.clip!!.zOrderTimeline != null) {
            this._zOrderTimeline = BaseObject.borrowObject(ZOrderTimelineState::class.java)
            this._zOrderTimeline!!.init(this._armature!!, this, this.clip!!.zOrderTimeline)
        }
    }

    /**
     * @private
     * @internal
     */
    fun updateTimelines() {
        val boneTimelines = HashMap<String, Array<BoneTimelineState>>()
        for (timeline in this._boneTimelines) { // Create bone timelines map.
            val timelineName = timeline.bone!!.name
            if (!boneTimelines.containsKey(timelineName)) {
                boneTimelines[timelineName] = Array()
            }

            boneTimelines[timelineName].add(timeline)
        }

        for (bone in this._armature!!.bones) {
            val timelineName = bone.name
            if (!this.containsBoneMask(timelineName)) {
                continue
            }

            val timelineDatas = this.clip!!.getBoneTimelines(timelineName)
            if (boneTimelines.containsKey(timelineName)) { // Remove bone timeline from map.
                boneTimelines.remove(timelineName)
            } else { // Create new bone timeline.
                if (!this._bonePoses.containsKey(timelineName)) {
                    this._bonePoses[timelineName] = BaseObject.borrowObject(BonePose::class.java)
                }
                val bonePose = this._bonePoses[timelineName]
                if (timelineDatas != null) {
                    for (timelineData in timelineDatas) {
                        when (timelineData.type) {
                            TimelineType.BoneAll -> {
                                val timeline = BaseObject.borrowObject(BoneAllTimelineState::class.java)
                                timeline.bone = bone
                                timeline.bonePose = bonePose
                                timeline.init(this._armature, this, timelineData)
                                this._boneTimelines.push(timeline)
                            }

                            TimelineType.BoneT, TimelineType.BoneR, TimelineType.BoneS -> {
                            }

                            TimelineType.BoneX, TimelineType.BoneY, TimelineType.BoneRotate, TimelineType.BoneSkew, TimelineType.BoneScaleX, TimelineType.BoneScaleY -> {
                            }
                        }// TODO
                        // TODO
                    }
                } else if (this.resetToPose) { // Pose timeline.
                    val timeline = BaseObject.borrowObject(BoneAllTimelineState::class.java)
                    timeline.bone = bone
                    timeline.bonePose = bonePose
                    timeline.init(this._armature, this, null)
                    this._boneTimelines.push(timeline)
                }
            }
        }

        for (k in boneTimelines.keys) { // Remove bone timelines.
            for (timeline in boneTimelines[k]) {
                this._boneTimelines.splice(this._boneTimelines.indexOfObject(timeline), 1)
                timeline.returnToPool()
            }
        }

        val slotTimelines = HashMap<String, Array<SlotTimelineState>>()
        val ffdFlags = IntArray()
        for (timeline in this._slotTimelines) { // Create slot timelines map.
            val timelineName = timeline.slot!!.name
            if (!slotTimelines.containsKey(timelineName)) {
                slotTimelines[timelineName] = Array()
            }

            slotTimelines[timelineName].add(timeline)
        }

        for (slot in this._armature!!.slots) {
            val boneName = slot.parent!!.name
            if (!this.containsBoneMask(boneName)) {
                continue
            }

            val timelineName = slot.name
            val timelineDatas = this.clip!!.getSlotTimeline(timelineName)
            if (slotTimelines.containsKey(timelineName)) {
                slotTimelines.remove(timelineName)
            } else { // Create new slot timeline.
                var displayIndexFlag = false
                var colorFlag = false
                ffdFlags.clear()

                if (timelineDatas != null) {
                    for (timelineData in timelineDatas) {
                        when (timelineData.type) {
                            TimelineType.SlotDisplay -> {
                                val timeline = BaseObject.borrowObject(SlotDislayIndexTimelineState::class.java)
                                timeline.slot = slot
                                timeline.init(this._armature, this, timelineData)
                                this._slotTimelines.push(timeline)
                                displayIndexFlag = true
                            }

                            TimelineType.SlotColor -> {
                                val timeline = BaseObject.borrowObject(SlotColorTimelineState::class.java)
                                timeline.slot = slot
                                timeline.init(this._armature, this, timelineData)
                                this._slotTimelines.push(timeline)
                                colorFlag = true
                            }

                            TimelineType.SlotFFD -> {
                                val timeline = BaseObject.borrowObject(SlotFFDTimelineState::class.java)
                                timeline.slot = slot
                                timeline.init(this._armature, this, timelineData)
                                this._slotTimelines.push(timeline)
                                ffdFlags.push(timeline.meshOffset)
                            }
                        }
                    }
                }

                if (this.resetToPose) { // Pose timeline.
                    if (!displayIndexFlag) {
                        val timeline = BaseObject.borrowObject(SlotDislayIndexTimelineState::class.java)
                        timeline.slot = slot
                        timeline.init(this._armature, this, null)
                        this._slotTimelines.push(timeline)
                    }

                    if (!colorFlag) {
                        val timeline = BaseObject.borrowObject(SlotColorTimelineState::class.java)
                        timeline.slot = slot
                        timeline.init(this._armature, this, null)
                        this._slotTimelines.push(timeline)
                    }

                    for (displayData in slot._rawDisplayDatas!!) {
                        if (displayData != null && displayData.type == DisplayType.Mesh && ffdFlags.indexOfObject((displayData as MeshDisplayData).offset) < 0) {
                            val timeline = BaseObject.borrowObject(SlotFFDTimelineState::class.java)
                            timeline.slot = slot
                            timeline.init(this._armature, this, null)
                            this._slotTimelines.push(timeline)
                        }
                    }
                }
            }
        }

        for (k in slotTimelines.keys) { // Remove slot timelines.
            for (timeline in slotTimelines[k]!!) {
                this._slotTimelines.splice(this._slotTimelines.indexOfObject(timeline), 1)
                timeline.returnToPool()
            }
        }
    }

    /**
     * @private
     * @internal
     */
    fun advanceTime(passedTime: Float, cacheFrameRate: Float) {
        var passedTime = passedTime
        // Update fade time.
        if (this._fadeState != 0f || this._subFadeState != 0f) {
            this._advanceFadeTime(passedTime)
        }

        // Update time.
        if (this._playheadState == 3) { // 11
            if (this.timeScale != 1f) {
                passedTime *= this.timeScale
            }

            this._time += passedTime
        }

        if (this._timelineDirty) {
            this._timelineDirty = false
            this.updateTimelines()
        }

        if (this.weight == 0f) {
            return
        }

        val isCacheEnabled = this._fadeState == 0f && cacheFrameRate > 0f
        var isUpdateTimeline = true
        var isUpdateBoneTimeline = true
        val time = this._time
        this._weightResult = this.weight * this._fadeProgress

        this._actionTimeline!!.update(time) // Update main timeline.

        if (isCacheEnabled) { // Cache time internval.
            val internval = cacheFrameRate * 2.0f
            this._actionTimeline!!.mcurrentTime =
                    (Math.floor((this._actionTimeline!!.mcurrentTime * internval).toDouble()) / internval).toFloat()
        }

        if (this._zOrderTimeline != null) { // Update zOrder timeline.
            this._zOrderTimeline!!.update(time)
        }

        if (isCacheEnabled) { // Update cache.
            val cacheFrameIndex =
                Math.floor((this._actionTimeline!!.mcurrentTime * cacheFrameRate).toDouble()).toInt() // uint
            if (this._armature!!._cacheFrameIndex == cacheFrameIndex) { // Same cache.
                isUpdateTimeline = false
                isUpdateBoneTimeline = false
            } else {
                this._armature!!._cacheFrameIndex = cacheFrameIndex
                if (this.clip!!.cachedFrames.getBool(cacheFrameIndex)) { // Cached.
                    isUpdateBoneTimeline = false
                } else { // Cache.
                    this.clip!!.cachedFrames.setBool(cacheFrameIndex, true)
                }
            }
        }

        if (isUpdateTimeline) {
            if (isUpdateBoneTimeline) { // Update bone timelines.
                var bone: Bone? = null
                var prevTimeline: BoneTimelineState? = null //
                var i = 0
                val l = this._boneTimelines.size()
                while (i < l) {
                    val timeline = this._boneTimelines.get(i)
                    if (bone !== timeline.bone) { // Blend bone pose.
                        if (bone != null) {
                            this._blendBoneTimline(prevTimeline!!)

                            if (bone._blendDirty) {
                                if (bone._blendLeftWeight > 0f) {
                                    if (bone._blendLayer != this.layer) {
                                        if (bone._blendLayerWeight >= bone._blendLeftWeight) {
                                            bone._blendLeftWeight = 0f
                                            bone = null
                                        } else {
                                            bone._blendLayer = this.layer
                                            bone._blendLeftWeight -= bone._blendLayerWeight
                                            bone._blendLayerWeight = 0f
                                        }
                                    }
                                } else {
                                    bone = null
                                }
                            }
                        }

                        bone = timeline.bone
                    }

                    if (bone != null) {
                        timeline.update(time)
                        if (i == l - 1) {
                            this._blendBoneTimline(timeline)
                        } else {
                            prevTimeline = timeline
                        }
                    }
                    ++i
                }
            }

            var i = 0
            val l = this._slotTimelines.size()
            while (i < l) {
                val timeline = this._slotTimelines.get(i)
                if (this._isDisabled(timeline.slot)) {
                    ++i
                    continue
                }

                timeline.update(time)
                ++i
            }
        }

        if (this._fadeState == 0f) {
            if (this._subFadeState > 0) {
                this._subFadeState = 0f
            }

            if (this._actionTimeline!!.playState > 0) {
                if (this.autoFadeOutTime >= 0f) { // Auto fade out.
                    this.fadeOut(this.autoFadeOutTime)
                }
            }
        }
    }

    /**
     * 继续播放。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun play() {
        this._playheadState = 3 // 11
    }

    /**
     * 暂停播放。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun stop() {
        this._playheadState = this._playheadState and 1 // 0x
    }

    /**
     * 淡出动画。
     *
     * @param fadeOutTime   淡出时间。 (以秒为单位)
     * @param pausePlayhead 淡出时是否暂停动画。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    @JvmOverloads
    fun fadeOut(fadeOutTime: Float, pausePlayhead: Boolean = true) {
        var fadeOutTime = fadeOutTime
        if (fadeOutTime < 0f) {
            fadeOutTime = 0f
        }

        if (pausePlayhead) {
            this._playheadState = this._playheadState and 2 // x0
        }

        if (this._fadeState > 0) {
            if (fadeOutTime > this.fadeTotalTime - this._fadeTime) { // If the animation is already in fade out, the new fade out will be ignored.
                return
            }
        } else {
            this._fadeState = 1f
            this._subFadeState = -1f

            if (fadeOutTime <= 0f || this._fadeProgress <= 0f) {
                this._fadeProgress = 0.000001f // Modify fade progress to different value.
            }

            for (timeline in this._boneTimelines) {
                timeline.fadeOut()
            }

            for (timeline in this._slotTimelines) {
                timeline.fadeOut()
            }
        }

        this.displayControl = false //
        this.fadeTotalTime = if (this._fadeProgress > 0.000001) fadeOutTime / this._fadeProgress else 0f
        this._fadeTime = this.fadeTotalTime * (1f - this._fadeProgress)
    }

    /**
     * 是否包含骨骼遮罩。
     *
     * @param name 指定的骨骼名称。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun containsBoneMask(name: String): Boolean {
        return this._boneMask.size() == 0 || this._boneMask.indexOf(name) >= 0
    }

    /**
     * 添加骨骼遮罩。
     *
     * @param name      指定的骨骼名称。
     * @param recursive 是否为该骨骼的子骨骼添加遮罩。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    @JvmOverloads
    fun addBoneMask(name: String, recursive: Boolean = true) {
        val currentBone = this._armature!!.getBone(name) ?: return

        if (this._boneMask.indexOf(name) < 0) { // Add mixing
            this._boneMask.add(name)
        }

        if (recursive) { // Add recursive mixing.
            for (bone in this._armature!!.bones) {
                if (this._boneMask.indexOf(bone.name) < 0 && currentBone.contains(bone)) {
                    this._boneMask.add(bone.name!!)
                }
            }
        }

        this._timelineDirty = true
    }

    /**
     * 删除骨骼遮罩。
     *
     * @param name      指定的骨骼名称。
     * @param recursive 是否删除该骨骼的子骨骼遮罩。
     * @version DragonBones 3.0
     * @language zh_CN
     */
    @JvmOverloads
    fun removeBoneMask(name: String, recursive: Boolean = true) {
        val index = this._boneMask.indexOf(name)
        if (index >= 0) { // Remove mixing.
            this._boneMask.splice(index, 1)
        }

        if (recursive) {
            val currentBone = this._armature!!.getBone(name)
            if (currentBone != null) {
                val bones = this._armature!!.bones
                if (this._boneMask.size() > 0) { // Remove recursive mixing.
                    for (bone in bones) {
                        val index2 = this._boneMask.indexOf(bone.name)
                        if (index2 >= 0 && currentBone.contains(bone)) {
                            this._boneMask.splice(index2, 1)
                        }
                    }
                } else { // Add unrecursive mixing.
                    for (bone in bones) {
                        if (bone === currentBone) {
                            continue
                        }

                        if (!currentBone.contains(bone)) {
                            this._boneMask.add(bone.name!!)
                        }
                    }
                }
            }
        }

        this._timelineDirty = true
    }

    /**
     * 删除所有骨骼遮罩。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    fun removeAllBoneMask() {
        this._boneMask.clear()
        this._timelineDirty = true
    }
}
