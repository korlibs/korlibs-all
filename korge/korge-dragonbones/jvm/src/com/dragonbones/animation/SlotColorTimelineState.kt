package com.dragonbones.animation

import com.dragonbones.geom.ColorTransform
import com.dragonbones.util.FloatArray
import com.dragonbones.util.ShortArray

/**
 * @internal
 * @private
 */
class SlotColorTimelineState : SlotTimelineState() {
    private var _dirty: Boolean = false
    private val _current = FloatArray(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    private val _delta = FloatArray(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    private val _result = FloatArray(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))

    override fun _onClear() {
        super._onClear()

        this._dirty = false
    }

    override fun _onArriveAtFrame() {
        super._onArriveAtFrame()

        if (this._timelineData != null) {
            val intArray = this._dragonBonesData!!.intArray
            val frameIntArray = this._dragonBonesData!!.frameIntArray
            val valueOffset =
                this._animationData!!.frameIntOffset + this._frameValueOffset + this._frameIndex * 1 // ...(timeline value offset)|x|x|(Value offset)|(Next offset)|x|x|...
            var colorOffset = frameIntArray!!.get(valueOffset)
            this._current.set(0, intArray!!.get(colorOffset++).toFloat())
            this._current.set(1, intArray.get(colorOffset++).toFloat())
            this._current.set(2, intArray.get(colorOffset++).toFloat())
            this._current.set(3, intArray.get(colorOffset++).toFloat())
            this._current.set(4, intArray.get(colorOffset++).toFloat())
            this._current.set(5, intArray.get(colorOffset++).toFloat())
            this._current.set(6, intArray.get(colorOffset++).toFloat())
            this._current.set(7, intArray.get(colorOffset++).toFloat())

            if (this._tweenState == TweenState.Always) {
                if (this._frameIndex == this._frameCount - 1) {
                    colorOffset = frameIntArray.get(this._animationData!!.frameIntOffset + this._frameValueOffset)
                } else {
                    colorOffset = frameIntArray.get(valueOffset + 1 * 1)
                }

                this._delta.set(0, intArray.get(colorOffset++) - this._current.get(0))
                this._delta.set(1, intArray.get(colorOffset++) - this._current.get(1))
                this._delta.set(2, intArray.get(colorOffset++) - this._current.get(2))
                this._delta.set(3, intArray.get(colorOffset++) - this._current.get(3))
                this._delta.set(4, intArray.get(colorOffset++) - this._current.get(4))
                this._delta.set(5, intArray.get(colorOffset++) - this._current.get(5))
                this._delta.set(6, intArray.get(colorOffset++) - this._current.get(6))
                this._delta.set(7, intArray.get(colorOffset++) - this._current.get(7))
            }
        } else { // Pose.
            val color = this.slot!!.slotData!!.color
            this._current.set(0, color!!.alphaMultiplier * 100f)
            this._current.set(1, color.redMultiplier * 100f)
            this._current.set(2, color.greenMultiplier * 100f)
            this._current.set(3, color.blueMultiplier * 100f)
            this._current.set(4, color.alphaOffset.toFloat())
            this._current.set(5, color.redOffset.toFloat())
            this._current.set(6, color.greenOffset.toFloat())
            this._current.set(7, color.blueOffset.toFloat())
        }
    }

    override fun _onUpdateFrame() {
        super._onUpdateFrame()

        this._dirty = true
        if (this._tweenState != TweenState.Always) {
            this._tweenState = TweenState.None
        }

        this._result.set(0, (this._current.get(0) + this._delta.get(0) * this._tweenProgress) * 0.01f)
        this._result.set(1, (this._current.get(1) + this._delta.get(1) * this._tweenProgress) * 0.01f)
        this._result.set(2, (this._current.get(2) + this._delta.get(2) * this._tweenProgress) * 0.01f)
        this._result.set(3, (this._current.get(3) + this._delta.get(3) * this._tweenProgress) * 0.01f)
        this._result.set(4, this._current.get(4) + this._delta.get(4) * this._tweenProgress)
        this._result.set(5, this._current.get(5) + this._delta.get(5) * this._tweenProgress)
        this._result.set(6, this._current.get(6) + this._delta.get(6) * this._tweenProgress)
        this._result.set(7, this._current.get(7) + this._delta.get(7) * this._tweenProgress)
    }

    override fun fadeOut() {
        this._tweenState = TweenState.None
        this._dirty = false
    }

    override fun update(passedTime: Float) {
        super.update(passedTime)

        // Fade animation.
        if (this._tweenState != TweenState.None || this._dirty) {
            val result = this.slot!!._colorTransform

            if (this._animationState!!._fadeState != 0f || this._animationState!!._subFadeState != 0f) {
                if (result.alphaMultiplier != this._result.get(0) ||
                    result.redMultiplier != this._result.get(1) ||
                    result.greenMultiplier != this._result.get(2) ||
                    result.blueMultiplier != this._result.get(3) ||
                    result.alphaOffset.toFloat() != this._result.get(4) ||
                    result.redOffset.toFloat() != this._result.get(5) ||
                    result.greenOffset.toFloat() != this._result.get(6) ||
                    result.blueOffset.toFloat() != this._result.get(7)
                ) {
                    val fadeProgress = Math.pow(this._animationState!!._fadeProgress.toDouble(), 4.0).toFloat()

                    result.alphaMultiplier += (this._result.get(0) - result.alphaMultiplier) * fadeProgress
                    result.redMultiplier += (this._result.get(1) - result.redMultiplier) * fadeProgress
                    result.greenMultiplier += (this._result.get(2) - result.greenMultiplier) * fadeProgress
                    result.blueMultiplier += (this._result.get(3) - result.blueMultiplier) * fadeProgress
                    result.alphaOffset += ((this._result.get(4) - result.alphaOffset) * fadeProgress).toInt()
                    result.redOffset += ((this._result.get(5) - result.redOffset) * fadeProgress).toInt()
                    result.greenOffset += ((this._result.get(6) - result.greenOffset) * fadeProgress).toInt()
                    result.blueOffset += ((this._result.get(7) - result.blueOffset) * fadeProgress).toInt()

                    this.slot!!._colorDirty = true
                }
            } else if (this._dirty) {
                this._dirty = false
                if (result.alphaMultiplier != this._result.get(1) ||
                    result.redMultiplier != this._result.get(1) ||
                    result.greenMultiplier != this._result.get(1) ||
                    result.blueMultiplier != this._result.get(1) ||
                    result.alphaOffset.toFloat() != this._result.get(1) ||
                    result.redOffset.toFloat() != this._result.get(1) ||
                    result.greenOffset.toFloat() != this._result.get(1) ||
                    result.blueOffset.toFloat() != this._result.get(1)
                ) {
                    result.alphaMultiplier = this._result.get(1)
                    result.redMultiplier = this._result.get(1)
                    result.greenMultiplier = this._result.get(1)
                    result.blueMultiplier = this._result.get(1)
                    result.alphaOffset = this._result.get(1).toInt()
                    result.redOffset = this._result.get(1).toInt()
                    result.greenOffset = this._result.get(1).toInt()
                    result.blueOffset = this._result.get(1).toInt()

                    this.slot!!._colorDirty = true
                }
            }
        }
    }
}
