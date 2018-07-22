package com.dragonbones.animation

import com.dragonbones.geom.Transform
import com.dragonbones.util.FloatArray

/**
 * @internal
 * @private
 */
class BoneAllTimelineState : BoneTimelineState() {
    override fun _onArriveAtFrame() {
        super._onArriveAtFrame()

        if (this._timelineData != null) {
            val frameFloatArray = this._dragonBonesData!!.frameFloatArray
            val current = this.bonePose!!.current
            val delta = this.bonePose!!.delta
            var valueOffset =
                this._animationData!!.frameFloatOffset + this._frameValueOffset + this._frameIndex * 6 // ...(timeline value offset)|xxxxxx|xxxxxx|(Value offset)xxxxx|(Next offset)xxxxx|xxxxxx|xxxxxx|...

            current.x = frameFloatArray!!.get(valueOffset++)
            current.y = frameFloatArray.get(valueOffset++)
            current.rotation = frameFloatArray.get(valueOffset++)
            current.skew = frameFloatArray.get(valueOffset++)
            current.scaleX = frameFloatArray.get(valueOffset++)
            current.scaleY = frameFloatArray.get(valueOffset++)

            if (this._tweenState == TweenState.Always) {
                if (this._frameIndex == this._frameCount - 1) {
                    valueOffset = this._animationData!!.frameFloatOffset + this._frameValueOffset
                }

                delta.x = frameFloatArray.get(valueOffset++) - current.x
                delta.y = frameFloatArray.get(valueOffset++) - current.y
                delta.rotation = frameFloatArray.get(valueOffset++) - current.rotation
                delta.skew = frameFloatArray.get(valueOffset++) - current.skew
                delta.scaleX = frameFloatArray.get(valueOffset++) - current.scaleX
                delta.scaleY = frameFloatArray.get(valueOffset++) - current.scaleY
            }
            // else {
            //     delta.x = 0f;
            //     delta.y = 0f;
            //     delta.rotation = 0f;
            //     delta.skew = 0f;
            //     delta.scaleX = 0f;
            //     delta.scaleY = 0f;
            // }
        } else { // Pose.
            val current = this.bonePose!!.current
            current.x = 0f
            current.y = 0f
            current.rotation = 0f
            current.skew = 0f
            current.scaleX = 1f
            current.scaleY = 1f
        }
    }

    override fun _onUpdateFrame() {
        super._onUpdateFrame()

        val current = this.bonePose!!.current
        val delta = this.bonePose!!.delta
        val result = this.bonePose!!.result

        this.bone!!._transformDirty = true
        if (this._tweenState != TweenState.Always) {
            this._tweenState = TweenState.None
        }

        val scale = this._armature!!.armatureData!!.scale
        result.x = (current.x + delta.x * this._tweenProgress) * scale
        result.y = (current.y + delta.y * this._tweenProgress) * scale
        result.rotation = current.rotation + delta.rotation * this._tweenProgress
        result.skew = current.skew + delta.skew * this._tweenProgress
        result.scaleX = current.scaleX + delta.scaleX * this._tweenProgress
        result.scaleY = current.scaleY + delta.scaleY * this._tweenProgress
    }

    override fun fadeOut() {
        val result = this.bonePose!!.result
        result.rotation = Transform.normalizeRadian(result.rotation)
        result.skew = Transform.normalizeRadian(result.skew)
    }
}
