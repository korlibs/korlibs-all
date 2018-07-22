package com.dragonbones.animation

import com.dragonbones.core.BinaryOffset
import com.dragonbones.core.TweenType
import com.dragonbones.util.ShortArray

/**
 * @internal
 * @private
 */
abstract class TweenTimelineState : TimelineState() {

    protected var _tweenType: TweenType = TweenType.None
    protected var _curveCount: Float = 0.toFloat()
    protected var _framePosition: Float = 0.toFloat()
    protected var _frameDurationR: Float = 0.toFloat()
    protected var _tweenProgress: Float = 0.toFloat()
    protected var _tweenEasing: Float = 0.toFloat()
    private fun _getEasingValue(tweenType: TweenType, progress: Float, easing: Float): Float {
        var value = progress

        when (tweenType) {
            TweenType.QuadIn -> value = Math.pow(progress.toDouble(), 2.0).toFloat()

            TweenType.QuadOut -> value = (1f - Math.pow((1f - progress).toDouble(), 2.0)).toFloat()

            TweenType.QuadInOut -> value = (0.5 * (1f - Math.cos(progress * Math.PI))).toFloat()
        }

        return (value - progress) * easing + progress
    }

    private fun _getEasingCurveValue(progress: Float, samples: ShortArray?, count: Float, offset: Int): Float {
        if (progress <= 0f) {
            return 0f
        } else if (progress >= 1f) {
            return 1f
        }

        val segmentCount = count + 1 // + 2 - 1
        val valueIndex = Math.floor((progress * segmentCount).toDouble()).toInt()
        val fromValue = if (valueIndex == 0) 0f else samples!!.get(offset + valueIndex - 1).toFloat()
        val toValue = if (valueIndex.toFloat() == segmentCount - 1) 10000f else samples!!.get(offset + valueIndex).toFloat()
        return (fromValue + (toValue - fromValue) * (progress * segmentCount - valueIndex)) * 0.0001f
    }

    override fun _onClear() {
        super._onClear()

        this._tweenType = TweenType.None
        this._curveCount = 0f
        this._framePosition = 0f
        this._frameDurationR = 0f
        this._tweenProgress = 0f
        this._tweenEasing = 0f
    }

    override fun _onArriveAtFrame() {
        if (this._frameCount > 1 && (this._frameIndex != this._frameCount - 1 ||
                    this._animationState!!.playTimes == 0 ||
                    this._animationState!!.currentPlayTimes < this._animationState!!.playTimes - 1)
        ) {
            this._tweenType =
                    TweenType.values[this._frameArray!!.get(this._frameOffset + BinaryOffset.FrameTweenType.v)] // TODO recode ture tween type.
            this._tweenState = if (this._tweenType == TweenType.None) TweenState.Once else TweenState.Always
            if (this._tweenType == TweenType.Curve) {
                this._curveCount =
                        this._frameArray!!.get(this._frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount.v)
                            .toFloat()
            } else if (this._tweenType != TweenType.None && this._tweenType != TweenType.Line) {
                this._tweenEasing = this._frameArray!!.get(this._frameOffset + BinaryOffset.FrameTweenEasingOrCurveSampleCount.v) *
                        0.01f
            }

            this._framePosition = this._frameArray!!.get(this._frameOffset) * this._frameRateR
            if (this._frameIndex == this._frameCount - 1) {
                this._frameDurationR = 1f / (this._animationData!!.duration - this._framePosition)
            } else {
                val nextFrameOffset =
                    this._animationData!!.frameOffset + this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineFrameOffset.v + this._frameIndex + 1)
                this._frameDurationR = 1f /
                        (this._frameArray!!.get(nextFrameOffset) * this._frameRateR - this._framePosition)
            }
        } else {
            this._tweenState = TweenState.Once
        }
    }

    override fun _onUpdateFrame() {
        if (this._tweenState == TweenState.Always) {
            this._tweenProgress = (this.mcurrentTime - this._framePosition) * this._frameDurationR
            if (this._tweenType == TweenType.Curve) {
                this._tweenProgress = _getEasingCurveValue(
                    this._tweenProgress,
                    this._frameArray,
                    this._curveCount,
                    this._frameOffset + BinaryOffset.FrameCurveSamples.v
                )
            } else if (this._tweenType != TweenType.Line) {
                this._tweenProgress = _getEasingValue(this._tweenType, this._tweenProgress, this._tweenEasing)
            }
        } else {
            this._tweenProgress = 0f
        }
    }
}
