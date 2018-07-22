package com.dragonbones.animation

import com.dragonbones.armature.Armature
import com.dragonbones.core.BaseObject
import com.dragonbones.core.BinaryOffset
import com.dragonbones.model.AnimationData
import com.dragonbones.model.DragonBonesData
import com.dragonbones.model.TimelineData
import com.dragonbones.util.CharArray
import com.dragonbones.util.FloatArray
import com.dragonbones.util.IntArray
import com.dragonbones.util.ShortArray

/**
 * @internal
 * @private
 */
abstract class TimelineState : BaseObject() {
    var playState: Int = 0 // -1: start, 0: play, 1: complete;
    var currentPlayTimes: Int = 0
    var mcurrentTime: Float = 0.toFloat()

    protected var _tweenState: TweenState = TweenState.None
    protected var _frameRate: Float = 0.toFloat()
    protected var _frameValueOffset: Int = 0
    protected var _frameCount: Int = 0
    protected var _frameOffset: Int = 0
    protected var _frameIndex: Int = 0
    protected var _frameRateR: Float = 0.toFloat()
    protected var _position: Float = 0.toFloat()
    protected var _duration: Float = 0.toFloat()
    protected var _timeScale: Float = 0.toFloat()
    protected var _timeOffset: Float = 0.toFloat()
    protected var _dragonBonesData: DragonBonesData? = null
    protected var _animationData: AnimationData? = null
    protected var _timelineData: TimelineData? = null
    protected var _armature: Armature? = null
    protected var _animationState: AnimationState? = null
    protected var _actionTimeline: TimelineState? = null
    protected var _frameArray: ShortArray? = null
    protected var _frameIntArray: ShortArray? = null
    protected var _frameFloatArray: FloatArray? = null
    protected var _timelineArray: CharArray? = null
    protected var _frameIndices: IntArray? = null

    override fun _onClear() {
        this.playState = -1
        this.currentPlayTimes = -1
        this.mcurrentTime = -1f

        this._tweenState = TweenState.None
        this._frameRate = 0f
        this._frameValueOffset = 0
        this._frameCount = 0
        this._frameOffset = 0
        this._frameIndex = -1
        this._frameRateR = 0f
        this._position = 0f
        this._duration = 0f
        this._timeScale = 1f
        this._timeOffset = 0f
        this._dragonBonesData = null //
        this._animationData = null //
        this._timelineData = null //
        this._armature = null //
        this._animationState = null //
        this._actionTimeline = null //
        this._frameArray = null //
        this._frameIntArray = null //
        this._frameFloatArray = null //
        this._timelineArray = null //
        this._frameIndices = null //
    }

    protected abstract fun _onArriveAtFrame()

    protected abstract fun _onUpdateFrame()

    protected fun _setCurrentTime(passedTime: Float): Boolean {
        var passedTime = passedTime
        val prevState = this.playState.toFloat()
        val prevPlayTimes = this.currentPlayTimes.toFloat()
        val prevTime = this.mcurrentTime

        if (this._actionTimeline != null && this._frameCount <= 1) { // No frame or only one frame.
            this.playState = if (this._actionTimeline!!.playState >= 0) 1 else -1
            this.currentPlayTimes = 1
            this.mcurrentTime = this._actionTimeline!!.mcurrentTime
        } else if (this._actionTimeline == null || this._timeScale != 1f || this._timeOffset != 0f) { // Action timeline or has scale and offset.
            val playTimes = this._animationState!!.playTimes
            val totalTime = playTimes * this._duration

            passedTime *= this._timeScale
            if (this._timeOffset != 0f) {
                passedTime += this._timeOffset * this._animationData!!.duration
            }

            if (playTimes > 0 && (passedTime >= totalTime || passedTime <= -totalTime)) {
                if (this.playState <= 0 && this._animationState!!._playheadState == 3) {
                    this.playState = 1
                }

                this.currentPlayTimes = playTimes
                if (passedTime < 0f) {
                    this.mcurrentTime = 0f
                } else {
                    this.mcurrentTime = this._duration
                }
            } else {
                if (this.playState != 0 && this._animationState!!._playheadState == 3) {
                    this.playState = 0
                }

                if (passedTime < 0f) {
                    passedTime = -passedTime
                    this.currentPlayTimes = Math.floor((passedTime / this._duration).toDouble()).toInt()
                    this.mcurrentTime = this._duration - passedTime % this._duration
                } else {
                    this.currentPlayTimes = Math.floor((passedTime / this._duration).toDouble()).toInt()
                    this.mcurrentTime = passedTime % this._duration
                }
            }

            this.mcurrentTime += this._position
        } else { // Multi frames.
            this.playState = this._actionTimeline!!.playState
            this.currentPlayTimes = this._actionTimeline!!.currentPlayTimes
            this.mcurrentTime = this._actionTimeline!!.mcurrentTime
        }

        if (this.currentPlayTimes.toFloat() == prevPlayTimes && this.mcurrentTime == prevTime) {
            return false
        }

        // Clear frame flag when timeline start or loopComplete.
        if (prevState < 0 && this.playState.toFloat() != prevState || this.playState <= 0 && this.currentPlayTimes.toFloat() != prevPlayTimes) {
            this._frameIndex = -1
        }

        return true
    }

    open fun init(armature: Armature?, animationState: AnimationState, timelineData: TimelineData?) {
        this._armature = armature
        this._animationState = animationState
        this._timelineData = timelineData
        this._actionTimeline = this._animationState!!._actionTimeline

        if (this === this._actionTimeline) {
            this._actionTimeline = null //
        }

        this._frameRate = this._armature!!.armatureData!!.frameRate
        this._frameRateR = 1f / this._frameRate
        this._position = this._animationState!!._position
        this._duration = this._animationState!!.totalTime
        this._dragonBonesData = this._armature!!.armatureData!!.parent
        this._animationData = this._animationState!!.clip

        if (this._timelineData != null) {
            this._frameIntArray = this._dragonBonesData!!.frameIntArray
            this._frameFloatArray = this._dragonBonesData!!.frameFloatArray
            this._frameArray = this._dragonBonesData!!.frameArray
            this._timelineArray = this._dragonBonesData!!.timelineArray
            this._frameIndices = this._dragonBonesData!!.frameIndices

            this._frameCount =
                    this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineKeyFrameCount.v)
            this._frameValueOffset =
                    this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineFrameValueOffset.v)
            this._timeScale = 100f /
                    this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineScale.v)
            this._timeOffset = this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineOffset.v) *
                    0.01f
        }
    }

    open fun fadeOut() {}

    open fun update(passedTime: Float) {
        if (this.playState <= 0 && this._setCurrentTime(passedTime)) {
            if (this._frameCount > 1) {
                val timelineFrameIndex = Math.floor((this.mcurrentTime * this._frameRate).toDouble()).toInt() // uint
                val frameIndex = this._frameIndices!!.get(this._timelineData!!.frameIndicesOffset + timelineFrameIndex)
                if (this._frameIndex != frameIndex) {
                    this._frameIndex = frameIndex
                    this._frameOffset = this._animationData!!.frameOffset +
                            this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineFrameOffset.v + this._frameIndex)

                    this._onArriveAtFrame()
                }
            } else if (this._frameIndex < 0) {
                this._frameIndex = 0
                if (this._timelineData != null) { // May be pose timeline.
                    this._frameOffset = this._animationData!!.frameOffset +
                            this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineFrameOffset.v)
                }

                this._onArriveAtFrame()
            }

            if (this._tweenState != TweenState.None) {
                this._onUpdateFrame()
            }
        }
    }
}


