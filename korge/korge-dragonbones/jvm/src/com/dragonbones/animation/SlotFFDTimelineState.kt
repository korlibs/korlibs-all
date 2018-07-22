package com.dragonbones.animation

import com.dragonbones.armature.Armature
import com.dragonbones.core.BinaryOffset
import com.dragonbones.model.TimelineData
import com.dragonbones.util.FloatArray
import com.dragonbones.util.ShortArray

/**
 * @internal
 * @private
 */
class SlotFFDTimelineState : SlotTimelineState() {
    var meshOffset: Int = 0

    private var _dirty: Boolean = false
    private var _frameFloatOffset: Int = 0
    private var _valueCount: Int = 0
    private var _ffdCount: Float = 0.toFloat()
    private var _valueOffset: Int = 0
    private val _current = FloatArray()
    private val _delta = FloatArray()
    private val _result = FloatArray()

    override fun _onClear() {
        super._onClear()

        this.meshOffset = 0

        this._dirty = false
        this._frameFloatOffset = 0
        this._valueCount = 0
        this._ffdCount = 0f
        this._valueOffset = 0
        this._current.clear()
        this._delta.clear()
        this._result.clear()
    }

    override fun _onArriveAtFrame() {
        super._onArriveAtFrame()

        if (this._timelineData != null) {
            val isTween = this._tweenState == TweenState.Always
            val frameFloatArray = this._dragonBonesData!!.frameFloatArray
            val valueOffset =
                this._animationData!!.frameFloatOffset + this._frameValueOffset + this._frameIndex * this._valueCount

            if (isTween) {
                var nextValueOffset = valueOffset + this._valueCount
                if (this._frameIndex == this._frameCount - 1) {
                    nextValueOffset = this._animationData!!.frameFloatOffset + this._frameValueOffset
                }

                for (i in 0 until this._valueCount) {
                    val v = frameFloatArray!!.get(valueOffset + i)
                    this._current.set(i, v)
                    this._delta.set(i, frameFloatArray.get(nextValueOffset + i) - v)
                }
            } else {
                for (i in 0 until this._valueCount) {
                    this._current.set(i, frameFloatArray!!.get(valueOffset + i))
                }
            }
        } else {
            for (i in 0 until this._valueCount) {
                this._current.set(i, 0f)
            }
        }
    }

    override fun _onUpdateFrame() {
        super._onUpdateFrame()

        this._dirty = true
        if (this._tweenState != TweenState.Always) {
            this._tweenState = TweenState.None
        }

        for (i in 0 until this._valueCount) {
            this._result.set(i, this._current.get(i) + this._delta.get(i) * this._tweenProgress)
        }
    }

    override fun init(armature: Armature, animationState: AnimationState, timelineData: TimelineData?) {
        super.init(armature, animationState, timelineData)

        if (this._timelineData != null) {
            val frameIntArray = this._dragonBonesData!!.frameIntArray
            val frameIntOffset =
                this._animationData!!.frameIntOffset + this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineFrameValueCount.v)
            this.meshOffset = frameIntArray!!.get(frameIntOffset + BinaryOffset.FFDTimelineMeshOffset.v)
            this._ffdCount = frameIntArray.get(frameIntOffset + BinaryOffset.FFDTimelineFFDCount.v).toFloat()
            this._valueCount = frameIntArray.get(frameIntOffset + BinaryOffset.FFDTimelineValueCount.v)
            this._valueOffset = frameIntArray.get(frameIntOffset + BinaryOffset.FFDTimelineValueOffset.v)
            this._frameFloatOffset = frameIntArray.get(frameIntOffset + BinaryOffset.FFDTimelineFloatOffset.v) +
                    this._animationData!!.frameFloatOffset
        } else {
            this._valueCount = 0
        }

        this._current.length = this._valueCount
        this._delta.length = this._valueCount
        this._result.length = this._valueCount

        for (i in 0 until this._valueCount) {
            this._delta.set(i, 0f)
        }
    }

    override fun fadeOut() {
        this._tweenState = TweenState.None
        this._dirty = false
    }

    override fun update(passedTime: Float) {
        if (this.slot!!._meshData == null || this._timelineData != null && this.slot!!._meshData!!.offset != this.meshOffset) {
            return
        }

        super.update(passedTime)

        // Fade animation.
        if (this._tweenState != TweenState.None || this._dirty) {
            val result = this.slot!!._ffdVertices
            if (this._timelineData != null) {
                val frameFloatArray = this._dragonBonesData!!.frameFloatArray
                if (this._animationState!!._fadeState != 0f || this._animationState!!._subFadeState != 0f) {
                    val fadeProgress = Math.pow(this._animationState!!._fadeProgress.toDouble(), 2.0).toFloat()

                    var i = 0
                    while (i < this._ffdCount) {
                        if (i < this._valueOffset) {
                            result.set(
                                i,
                                result.get(i) + (frameFloatArray!!.get(this._frameFloatOffset + i) - result.get(i)) * fadeProgress
                            )
                        } else if (i < this._valueOffset + this._valueCount) {
                            result.set(
                                i,
                                result.get(i) + (this._result.get(i - this._valueOffset) - result.get(i)) * fadeProgress
                            )
                        } else {
                            result.set(
                                i,
                                result.get(i) + (frameFloatArray!!.get(this._frameFloatOffset + i - this._valueCount) - result.get(
                                    i
                                )) * fadeProgress
                            )
                        }
                        ++i
                    }

                    this.slot!!._meshDirty = true
                } else if (this._dirty) {
                    this._dirty = false

                    var i = 0
                    while (i < this._ffdCount) {
                        if (i < this._valueOffset) {
                            result.set(i, frameFloatArray!!.get(this._frameFloatOffset + i))
                        } else if (i < this._valueOffset + this._valueCount) {
                            result.set(i, this._result.get(i - this._valueOffset))
                        } else {
                            result.set(i, frameFloatArray!!.get(this._frameFloatOffset + i - this._valueCount))
                        }
                        ++i
                    }

                    this.slot!!._meshDirty = true
                }
            } else {
                this._ffdCount = result.size().toFloat() //
                if (this._animationState!!._fadeState != 0f || this._animationState!!._subFadeState != 0f) {
                    val fadeProgress = Math.pow(this._animationState!!._fadeProgress.toDouble(), 2.0).toFloat()
                    var i = 0
                    while (i < this._ffdCount) {
                        result.set(i, result.get(i) + (0f - result.get(i)) * fadeProgress)
                        ++i
                    }

                    this.slot!!._meshDirty = true
                } else if (this._dirty) {
                    this._dirty = false

                    var i = 0
                    while (i < this._ffdCount) {
                        result.set(i, 0f)
                        ++i
                    }

                    this.slot!!._meshDirty = true
                }
            }
        }
    }
}
