package com.dragonbones.model

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import com.dragonbones.core.*

/**
 * - The animation data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 动画数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
class AnimationData  :  BaseObject() {
	public override fun toString(): String {
		return "[class dragonBones.AnimationData]"
	}
	/**
	 * - FrameIntArray.
	 * @internal
	 */
	public var frameIntOffset: Int
	/**
	 * - FrameFloatArray.
	 * @internal
	 */
	public var frameFloatOffset: Int
	/**
	 * - FrameArray.
	 * @internal
	 */
	public var frameOffset: Int
	/**
	 * @private
	 */
	public var blendType: AnimationBlendType
	/**
	 * - The frame count of the animation.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画的帧数。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var frameCount: Int
	/**
	 * - The play times of the animation. [0: Loop play, [1~N]: Play N times]
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画的播放次数。 [0: 无限循环播放, [1~N]: 循环播放 N 次]
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var playTimes: Int
	/**
	 * - The duration of the animation. (In seconds)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画的持续时间。 （以秒为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var duration: Double
	/**
	 * @private
	 */
	public var scale: Double
	/**
	 * - The fade in time of the animation. (In seconds)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画的淡入时间。 （以秒为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var fadeInTime: Double
	/**
	 * @private
	 */
	public var cacheFrameRate: Double
	/**
	 * - The animation name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 动画名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var name: String
	/**
	 * @private
	 */
	public val cachedFrames: Array<boolean> = []
	/**
	 * @private
	 */
	public val boneTimelines: Map<Array<TimelineData>> = {}
	/**
	 * @private
	 */
	public val slotTimelines: Map<Array<TimelineData>> = {}
	/**
	 * @private
	 */
	public val constraintTimelines: Map<Array<TimelineData>> = {}
	/**
	 * @private
	 */
	public val animationTimelines: Map<Array<TimelineData>> = {}
	/**
	 * @private
	 */
	public val boneCachedFrameIndices: Map<Array<number>> = {}
	/**
	 * @private
	 */
	public val slotCachedFrameIndices: Map<Array<number>> = {}
	/**
	 * @private
	 */
	public var actionTimeline: TimelineData? = null // Initial value.
	/**
	 * @private
	 */
	public var zOrderTimeline: TimelineData? = null // Initial value.
	/**
	 * @private
	 */
	public var parent: ArmatureData

	protected fun _onClear(): Unit {
		for (k in this.boneTimelines.keys) {
			for (timeline int this.boneTimelines[k]) {
				timeline.returnToPool()
			}

			delete this.boneTimelines[k]
		}

		for (k in this.slotTimelines.keys) {
			for (timeline in this.slotTimelines[k]) {
				timeline.returnToPool()
			}

			delete this.slotTimelines[k]
		}

		for (k in this.constraintTimelines.keys) {
			for (timeline in this.constraintTimelines[k]) {
				timeline.returnToPool()
			}

			delete this.constraintTimelines[k]
		}

		for (k in this.animationTimelines.keys) {
			for (timeline in this.animationTimelines[k]) {
				timeline.returnToPool()
			}

			delete this.animationTimelines[k]
		}

		for (k in this.boneCachedFrameIndices.keys) {
			delete this.boneCachedFrameIndices[k]
		}

		for (k in this.slotCachedFrameIndices) {
			delete this.slotCachedFrameIndices[k]
		}

		if (this.actionTimeline !== null) {
			this.actionTimeline.returnToPool()
		}

		if (this.zOrderTimeline !== null) {
			this.zOrderTimeline.returnToPool()
		}

		this.frameIntOffset = 0
		this.frameFloatOffset = 0
		this.frameOffset = 0
		this.blendType = AnimationBlendType.None
		this.frameCount = 0
		this.playTimes = 0
		this.duration = 0.0
		this.scale = 1.0
		this.fadeInTime = 0.0
		this.cacheFrameRate = 0.0
		this.name = ""
		this.cachedFrames.length = 0
		// this.boneTimelines.clear();
		// this.slotTimelines.clear();
		// this.constraintTimelines.clear();
		// this.animationTimelines.clear();
		// this.boneCachedFrameIndices.clear();
		// this.slotCachedFrameIndices.clear();
		this.actionTimeline = null
		this.zOrderTimeline = null
		this.parent = null as any //
	}
	/**
	 * @internal
	 */
	public fun cacheFrames(frameRate: Double): Unit {
		if (this.cacheFrameRate > 0.0) { // TODO clear cache.
			return
		}

		this.cacheFrameRate = Math.max(Math.ceil(frameRate * this.scale), 1.0)
		val cacheFrameCount = Math.ceil(this.cacheFrameRate * this.duration) + 1 // Cache one more frame.

		this.cachedFrames.length = cacheFrameCount
		for (i in 0 until this.cacheFrames.length) {
			this.cachedFrames[i] = false
		}

		for (bone in this.parent.sortedBones) {
			val indices = DoubleArray(cacheFrameCount)
			for (i in 0 until indices.length) {
				indices[i] = -1
			}

			this.boneCachedFrameIndices[bone.name] = indices
		}

		for (slot in this.parent.sortedSlots) {
			val indices =  DoubleArray(cacheFrameCount)
			for (i in 0 until indices.length) {
				indices[i] = -1
			}

			this.slotCachedFrameIndices[slot.name] = indices
		}
	}
	/**
	 * @private
	 */
	public fun addBoneTimeline(timelineName: String, timeline: TimelineData): Unit {
		val timelines = timelineName in this.boneTimelines ? this.boneTimelines[timelineName] : (this.boneTimelines[timelineName] = [])
		if (timelines.indexOf(timeline) < 0) {
			timelines.push(timeline)
		}
	}
	/**
	 * @private
	 */
	public fun addSlotTimeline(timelineName: String, timeline: TimelineData): Unit {
		val timelines = timelineName in this.slotTimelines ? this.slotTimelines[timelineName] : (this.slotTimelines[timelineName] = [])
		if (timelines.indexOf(timeline) < 0) {
			timelines.push(timeline)
		}
	}
	/**
	 * @private
	 */
	public fun addConstraintTimeline(timelineName: String, timeline: TimelineData): Unit {
		val timelines = timelineName in this.constraintTimelines ? this.constraintTimelines[timelineName] : (this.constraintTimelines[timelineName] = [])
		if (timelines.indexOf(timeline) < 0) {
			timelines.push(timeline)
		}
	}
	/**
	 * @private
	 */
	public fun addAnimationTimeline(timelineName: String, timeline: TimelineData): Unit {
		val timelines = timelineName in this.animationTimelines ? this.animationTimelines[timelineName] : (this.animationTimelines[timelineName] = [])
		if (timelines.indexOf(timeline) < 0) {
			timelines.push(timeline)
		}
	}
	/**
	 * @private
	 */
	public fun getBoneTimelines(timelineName: String): Array<TimelineData>? {
		return timelineName in this.boneTimelines ? this.boneTimelines[timelineName] : null
	}
	/**
	 * @private
	 */
	public fun getSlotTimelines(timelineName: String): Array<TimelineData>? {
		return timelineName in this.slotTimelines ? this.slotTimelines[timelineName] : null
	}
	/**
	 * @private
	 */
	public fun getConstraintTimelines(timelineName: String): Array<TimelineData>? {
		return timelineName in this.constraintTimelines ? this.constraintTimelines[timelineName] : null
	}
	/**
	 * @private
	 */
	public fun getAnimationTimelines(timelineName: String): Array<TimelineData>? {
		return timelineName in this.animationTimelines ? this.animationTimelines[timelineName] : null
	}
	/**
	 * @private
	 */
	public fun getBoneCachedFrameIndices(boneName: String):  DoubleArray? {
		return boneName in this.boneCachedFrameIndices ? this.boneCachedFrameIndices[boneName] : null
	}
	/**
	 * @private
	 */
	public fun getSlotCachedFrameIndices(slotName: String):  DoubleArray? {
		return slotName in this.slotCachedFrameIndices ? this.slotCachedFrameIndices[slotName] : null
	}
}
/**
 * @private
 */
class TimelineData  : BaseObject() {
	public override fun toString(): String {
		return "[class dragonBones.TimelineData]"
	}

	public var type: TimelineType
	public var offset: Double // TimelineArray.
	public var frameIndicesOffset: Double // FrameIndices.

	protected fun _onClear(): Unit {
		this.type = TimelineType.BoneAll
		this.offset = 0
		this.frameIndicesOffset = -1
	}
}
/**
 * @internal
 */
class AnimationTimelineData  :  TimelineData() {
	public override fun toString(): String {
		return "[class dragonBones.AnimationTimelineData]"
	}

	public var x: Double
	public var y: Double

	protected fun _onClear(): Unit {
		super._onClear()

		this.x = 0.0
		this.y = 0.0
	}
}
