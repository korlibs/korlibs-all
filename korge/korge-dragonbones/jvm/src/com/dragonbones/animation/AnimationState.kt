package com.dragonbones.animation

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

import com.dragonbones.armature.*
import com.dragonbones.core.*
import com.dragonbones.model.*
import com.dragonbones.util.*

/**
 * - The animation state is generated when the animation data is played.
 * @see dragonBones.Animation
 * @see dragonBones.AnimationData
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 动画状态由播放动画数据时产生。
 * @see dragonBones.Animation
 * @see dragonBones.AnimationData
 * @version DragonBones 3.0
 * @language zh_CN
 */
class AnimationState : BaseObject() {
	public override fun toString(): String {
		return "[class dragonBones.AnimationState]"
	}
	/**
	 * @private
	 */
	public var actionEnabled: Boolean = false
	/**
	 * @private
	 */
	public var additive: Boolean = false
	/**
	 * - Whether the animation state has control over the display object properties of the slots.
	 * Sometimes blend a animation state does not want it to control the display object properties of the slots,
	 * especially if other animation state are controlling the display object properties of the slots.
	 * @default true
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 动画状态是否对插槽的显示对象属性有控制权。
	 * 有时混合一个动画状态并不希望其控制插槽的显示对象属性，
	 * 尤其是其他动画状态正在控制这些插槽的显示对象属性时。
	 * @default true
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	public var displayControl: Boolean = false
	/**
	 * - Whether to reset the objects without animation to the armature pose when the animation state is start to play.
	 * This property should usually be set to false when blend multiple animation states.
	 * @default true
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 开始播放动画状态时是否将没有动画的对象重置为骨架初始值。
	 * 通常在混合多个动画状态时应该将该属性设置为 false。
	 * @default true
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	public var resetToPose: Boolean = false
	/**
	 * @private
	 */
	public var blendType: AnimationBlendType = AnimationBlendType.None
	/**
	 * - The play times. [0: Loop play, [1~N]: Play N times]
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 播放次数。 [0: 无限循环播放, [1~N]: 循环播放 N 次]
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var playTimes: Int = 1
	/**
	 * - The blend layer.
	 * High layer animation state will get the blend weight first.
	 * When the blend weight is assigned more than 1, the remaining animation states will no longer get the weight assigned.
	 * @readonly
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 混合图层。
	 * 图层高的动画状态会优先获取混合权重。
	 * 当混合权重分配超过 1 时，剩余的动画状态将不再获得权重分配。
	 * @readonly
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	public var layer: Int = 0
	/**
	 * - The play speed.
	 * The value is an overlay relationship with {@link dragonBones.Animation#timeScale}.
	 * [(-N~0): Reverse play, 0: Stop play, (0~1): Slow play, 1: Normal play, (1~N): Fast play]
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 播放速度。
	 * 该值与 {@link dragonBones.Animation#timeScale} 是叠加关系。
	 * [(-N~0): 倒转播放, 0: 停止播放, (0~1): 慢速播放, 1: 正常播放, (1~N): 快速播放]
	 * @default 1.0
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var timeScale: Double = 1.0
	/**
	 * @private
	 */
	public var parameterX: Double = 0.0
	/**
	 * @private
	 */
	public var parameterY: Double = 0.0
	/**
	 * @private
	 */
	public var positionX: Double = 0.0
	/**
	 * @private
	 */
	public var positionY: Double = 0.0
	/**
	 * - The auto fade out time when the animation state play completed.
	 * [-1: Do not fade out automatically, [0~N]: The fade out time] (In seconds)
	 * @default -1.0
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 动画状态播放完成后的自动淡出时间。
	 * [-1: 不自动淡出, [0~N]: 淡出时间] （以秒为单位）
	 * @default -1.0
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	public var autoFadeOutTime: Double = 0.0
	/**
	 * @private
	 */
	public var fadeTotalTime: Double = 0.0
	/**
	 * - The name of the animation state. (Can be different from the name of the animation data)
	 * @readonly
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 动画状态名称。 （可以不同于动画数据）
	 * @readonly
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	public var name: String = ""
	/**
	 * - The blend group name of the animation state.
	 * This property is typically used to specify the substitution of multiple animation states blend.
	 * @readonly
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 混合组名称。
	 * 该属性通常用来指定多个动画状态混合时的相互替换关系。
	 * @readonly
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	public var group: String = ""
	private var _timelineDirty: Int = 2
	/**
	 * - xx: Play Enabled, Fade Play Enabled
	 * @internal
	 */
	public var _playheadState: Int = 0
	/**
	 * -1: Fade in, 0: Fade complete, 1: Fade out;
	 * @internal
	 */
	public var _fadeState: Int = -1
	/**
	 * -1: Fade start, 0: Fading, 1: Fade complete;
	 * @internal
	 */
	public var _subFadeState: Int = -1
	/**
	 * @internal
	 */
	public var _position: Double = 0.0
	/**
	 * @internal
	 */
	public var _duration: Double = 0.0
	private var _weight: Double = 1.0
	private var _fadeTime: Double = 0.0
	private var _time: Double = 0.0
	/**
	 * @internal
	 */
	public var _fadeProgress: Double = 0.0
	/**
	 * @internal
	 */
	public var _weightResult: Double = 0.0
	private val _boneMask: ArrayList<String> = []
	private val _boneTimelines: ArrayList<TimelineState> = []
	private val _boneBlendTimelines: ArrayList<TimelineState> = []
	private val _slotTimelines: ArrayList<TimelineState> = []
	private val _slotBlendTimelines: ArrayList<TimelineState> = []
	private val _constraintTimelines: ArrayList<TimelineState> = []
	private val _animationTimelines: ArrayList<TimelineState> = []
	private val _poseTimelines: ArrayList<TimelineState> = []
	private var _animationData: AnimationData
	private var _armature: Armature
	/**
	 * @internal
	 */
	public var _actionTimeline: ActionTimelineState = null as any // Initial value.
	private var _zOrderTimeline: ZOrderTimelineState? = null // Initial value.
	private var _activeChildA: AnimationState?
	private var _activeChildB: AnimationState?
	/**
	 * @internal
	 */
	public var _parent: AnimationState?

	protected fun _onClear(): Unit {
		for (timeline in this._boneTimelines) {
			timeline.returnToPool()
		}

		for (timeline in this._boneBlendTimelines) {
			timeline.returnToPool()
		}

		for (timeline in this._slotTimelines) {
			timeline.returnToPool()
		}

		for (timeline in this._slotBlendTimelines) {
			timeline.returnToPool()
		}

		for (timeline in this._constraintTimelines) {
			timeline.returnToPool()
		}

		for (timeline in this._animationTimelines) {
			val animationState = timeline.target as AnimationState
			if (animationState._parent === this) {
				animationState._fadeState = 1
				animationState._subFadeState = 1
				animationState._parent = null
			}

			timeline.returnToPool()
		}

		if (this._actionTimeline != null) {
			this._actionTimeline.returnToPool()
		}

		if (this._zOrderTimeline !== null) {
			this._zOrderTimeline.returnToPool()
		}

		this.actionEnabled = false
		this.additive = false
		this.displayControl = false
		this.resetToPose = false
		this.blendType = AnimationBlendType.None
		this.playTimes = 1
		this.layer = 0
		this.timeScale = 1.0
		this._weight = 1.0
		this.parameterX = 0.0
		this.parameterY = 0.0
		this.positionX = 0.0
		this.positionY = 0.0
		this.autoFadeOutTime = 0.0
		this.fadeTotalTime = 0.0
		this.name = ""
		this.group = ""

		this._timelineDirty = 2
		this._playheadState = 0
		this._fadeState = -1
		this._subFadeState = -1
		this._position = 0.0
		this._duration = 0.0
		this._fadeTime = 0.0
		this._time = 0.0
		this._fadeProgress = 0.0
		this._weightResult = 0.0
		this._boneMask.clear()
		this._boneTimelines.clear()
		this._boneBlendTimelines.clear()
		this._slotTimelines.clear()
		this._slotBlendTimelines.clear()
		this._constraintTimelines.clear()
		this._animationTimelines.clear()
		this._poseTimelines.clear()
		// this._bonePoses.clear();
		this._animationData = null as any //
		this._armature = null as any //
		this._actionTimeline = null as any //
		this._zOrderTimeline = null
		this._activeChildA = null
		this._activeChildB = null
		this._parent = null
	}

	private fun _updateTimelines(): Unit {
		 // Update constraint timelines.
		for (constraint in this._armature._constraints) {
			val timelineDatas = this._animationData.getConstraintTimelines(constraint.name)

			if (timelineDatas !== null) {
				for (timelineData in timelineDatas) {
					when (timelineData.type) {
						TimelineType.IKConstraint -> {
							val timeline = BaseObject.borrowObject<IKConstraintTimelineState>()
							timeline.target = constraint
							timeline.init(this._armature, this, timelineData)
							this._constraintTimelines.push(timeline)
						}

						else -> Unit
					}
				}
			}
			else if (this.resetToPose) { // Pose timeline.
				val timeline = BaseObject.borrowObject<IKConstraintTimelineState>()
				timeline.target = constraint
				timeline.init(this._armature, this, null)
				this._constraintTimelines.push(timeline)
				this._poseTimelines.push(timeline)
			}
		}

	}

	private fun _updateBoneAndSlotTimelines(): Unit {
		run { // Update bone and surface timelines.
			val boneTimelines: LinkedHashMap<String, ArrayList<TimelineState>> = {}
			// Create bone timelines map.
			for (timeline in this._boneTimelines) {
				val timelineName = ((timeline.target as BlendState).target as Bone).name
				if (!(timelineName in boneTimelines)) {
					boneTimelines[timelineName] = []
				}

				boneTimelines[timelineName].push(timeline)
			}

			for (timeline in this._boneBlendTimelines) {
				val timelineName = ((timeline.target as BlendState).target as Bone).name
				if (!(timelineName in boneTimelines)) {
					boneTimelines[timelineName] = []
				}

				boneTimelines[timelineName].push(timeline)
			}
			//
			for (bone in this._armature.getBones()) {
				val timelineName = bone.name
				if (!this.containsBoneMask(timelineName)) {
					continue
				}

				if (timelineName in boneTimelines) { // Remove bone timeline from map.
					boneTimelines.remove(timelineName)
				}
				else { // Create new bone timeline.
					val timelineDatas = this._animationData.getBoneTimelines(timelineName)
					val blendState = this._armature.animation.getBlendState(BlendState.BONE_TRANSFORM, bone.name, bone)

					if (timelineDatas !== null) {
						for (timelineData in timelineDatas) {
							when (timelineData.type) {
								TimelineType.BoneAll -> {
									val timeline = BaseObject.borrowObject<BoneAllTimelineState>()
									timeline.target = blendState
									timeline.init(this._armature, this, timelineData)
									this._boneTimelines.push(timeline)
								}

								TimelineType.BoneTranslate -> {
									val timeline = BaseObject.borrowObject<BoneTranslateTimelineState>()
									timeline.target = blendState
									timeline.init(this._armature, this, timelineData)
									this._boneTimelines.push(timeline)
								}

								TimelineType.BoneRotate -> {
									val timeline = BaseObject.borrowObject<BoneRotateTimelineState>()
									timeline.target = blendState
									timeline.init(this._armature, this, timelineData)
									this._boneTimelines.push(timeline)
								}

								TimelineType.BoneScale -> {
									val timeline = BaseObject.borrowObject<BoneScaleTimelineState>()
									timeline.target = blendState
									timeline.init(this._armature, this, timelineData)
									this._boneTimelines.push(timeline)
								}

								TimelineType.BoneAlpha -> {
									val timeline = BaseObject.borrowObject<AlphaTimelineState>()
									timeline.target = this._armature.animation.getBlendState(BlendState.BONE_ALPHA, bone.name, bone)
									timeline.init(this._armature, this, timelineData)
									this._boneBlendTimelines.push(timeline)
								}

								TimelineType.Surface -> {
									val timeline = BaseObject.borrowObject<SurfaceTimelineState>()
									timeline.target = this._armature.animation.getBlendState(BlendState.SURFACE, bone.name, bone)
									timeline.init(this._armature, this, timelineData)
									this._boneBlendTimelines.push(timeline)
								}

								else -> Unit
							}
						}
					}
					else if (this.resetToPose) { // Pose timeline.
						if (bone._boneData.type === BoneType.Bone) {
							val timeline = BaseObject.borrowObject<BoneAllTimelineState>()
							timeline.target = blendState
							timeline.init(this._armature, this, null)
							this._boneTimelines.push(timeline)
							this._poseTimelines.push(timeline)
						}
						else {
							val timeline = BaseObject.borrowObject<SurfaceTimelineState>()
							timeline.target = this._armature.animation.getBlendState(BlendState.SURFACE, bone.name, bone)
							timeline.init(this._armature, this, null)
							this._boneBlendTimelines.push(timeline)
							this._poseTimelines.push(timeline)
						}
					}
				}
			}

			for (k in boneTimelines.keys) { // Remove bone timelines.
				for (timeline in boneTimelines[k]) {
					var index = this._boneTimelines.indexOf(timeline)
					if (index >= 0) {
						this._boneTimelines.splice(index, 1)
						timeline.returnToPool()
					}

					index = this._boneBlendTimelines.indexOf(timeline)
					if (index >= 0) {
						this._boneBlendTimelines.splice(index, 1)
						timeline.returnToPool()
					}
				}
			}
		}

		run { // Update slot timelines.
			val slotTimelines: Map<Array<TimelineState>> = {}
			val ffdFlags:  DoubleArray = []
			// Create slot timelines map.
			for (timeline in this._slotTimelines) {
				val timelineName = (timeline.target as Slot).name
				if (!(timelineName in slotTimelines)) {
					slotTimelines[timelineName] = []
				}

				slotTimelines[timelineName].push(timeline)
			}

			for (timeline in this._slotBlendTimelines) {
				val timelineName = ((timeline.target as BlendState).target as Slot).name
				if (!(timelineName in slotTimelines)) {
					slotTimelines[timelineName] = []
				}

				slotTimelines[timelineName].push(timeline)
			}
			//
			for (slot in this._armature.getSlots()) {
				val boneName = slot.parent.name
				if (!this.containsBoneMask(boneName)) {
					continue
				}

				val timelineName = slot.name
				if (timelineName in slotTimelines) { // Remove slot timeline from map.
					delete slotTimelines[timelineName]
				}
				else { // Create new slot timeline.
					var displayIndexFlag = false
					var colorFlag = false
					ffdFlags.length = 0

					val timelineDatas = this._animationData.getSlotTimelines(timelineName)
					if (timelineDatas !== null) {
						for (timelineData in timelineDatas) {
							when (timelineData.type) {
								TimelineType.SlotDisplay -> {
									val timeline = BaseObject.borrowObject<SlotDisplayTimelineState>()
									timeline.target = slot
									timeline.init(this._armature, this, timelineData)
									this._slotTimelines.push(timeline)
									displayIndexFlag = true
								}

								TimelineType.SlotZIndex -> {
									val timeline = BaseObject.borrowObject<SlotZIndexTimelineState>()
									timeline.target = this._armature.animation.getBlendState(BlendState.SLOT_Z_INDEX, slot.name, slot)
									timeline.init(this._armature, this, timelineData)
									this._slotBlendTimelines.push(timeline)
								}

								TimelineType.SlotColor -> {
									val timeline = BaseObject.borrowObject<SlotColorTimelineState>()
									timeline.target = slot
									timeline.init(this._armature, this, timelineData)
									this._slotTimelines.push(timeline)
									colorFlag = true
								}

								TimelineType.SlotDeform -> {
									val timeline = BaseObject.borrowObject<DeformTimelineState>()
									timeline.target = this._armature.animation.getBlendState(BlendState.SLOT_DEFORM, slot.name, slot)
									timeline.init(this._armature, this, timelineData)

									if (timeline.target !== null) {
										this._slotBlendTimelines.push(timeline)
										ffdFlags.push(timeline.geometryOffset)
									}
									else {
										timeline.returnToPool()
									}
								}
								TimelineType.SlotAlpha -> {
									val timeline = BaseObject.borrowObject<AlphaTimelineState>()
									timeline.target = this._armature.animation.getBlendState(BlendState.SLOT_ALPHA, slot.name, slot)
									timeline.init(this._armature, this, timelineData)
									this._slotBlendTimelines.push(timeline)
									break
								}

								else -> {

								}
							}
						}
					}

					if (this.resetToPose) { // Pose timeline.
						if (!displayIndexFlag) {
							val timeline = BaseObject.borrowObject<SlotDisplayTimelineState>()
							timeline.target = slot
							timeline.init(this._armature, this, null)
							this._slotTimelines.push(timeline)
							this._poseTimelines.push(timeline)
						}

						if (!colorFlag) {
							val timeline = BaseObject.borrowObject<SlotColorTimelineState>()
							timeline.target = slot
							timeline.init(this._armature, this, null)
							this._slotTimelines.push(timeline)
							this._poseTimelines.push(timeline)
						}

						for (i in 0 until slot.displayFrameCount) {
							val displayFrame = slot.getDisplayFrameAt(i)
							if (displayFrame.deformVertices.isEmpty()) {
								continue
							}

							val geometryData = displayFrame.getGeometryData()
							if (geometryData !== null && ffdFlags.indexOf(geometryData.offset) < 0) {
								val timeline = BaseObject.borrowObject<DeformTimelineState>()
								timeline.geometryOffset = geometryData.offset //
								timeline.displayFrame = displayFrame //
								timeline.target = this._armature.animation.getBlendState(BlendState.SLOT_DEFORM, slot.name, slot)
								timeline.init(this._armature, this, null)
								this._slotBlendTimelines.push(timeline)
								this._poseTimelines.push(timeline)
							}
						}
					}
				}
			}

			for (var k in slotTimelines) { // Remove slot timelines.
				for (timeline in slotTimelines[k]) {
					var index = this._slotTimelines.indexOf(timeline)
					if (index >= 0) {
						this._slotTimelines.splice(index, 1)
						timeline.returnToPool()
					}

					index = this._slotBlendTimelines.indexOf(timeline)
					if (index >= 0) {
						this._slotBlendTimelines.splice(index, 1)
						timeline.returnToPool()
					}
				}
			}
		}
	}

	private fun _advanceFadeTime(passedTime: Double): Unit {
		val isFadeOut = this._fadeState > 0

		if (this._subFadeState < 0) { // Fade start event.
			this._subFadeState = 0

			val eventActive = this._parent === null && this.actionEnabled
			if (eventActive) {
				val eventType = isFadeOut ? EventObject.FADE_OUT : EventObject.FADE_IN
				if (this._armature.eventDispatcher.hasDBEventListener(eventType)) {
					val eventObject = BaseObject.borrowObject<EventObject>()
					eventObject.type = eventType
					eventObject.armature = this._armature
					eventObject.animationState = this
					this._armature._dragonBones.bufferEvent(eventObject)
				}
			}
		}

		if (passedTime < 0.0) {
			passedTime = -passedTime
		}

		this._fadeTime += passedTime

		if (this._fadeTime >= this.fadeTotalTime) { // Fade complete.
			this._subFadeState = 1
			this._fadeProgress = isFadeOut ? 0.0 : 1.0
		}
		else if (this._fadeTime > 0.0) { // Fading.
			this._fadeProgress = isFadeOut ? (1.0 - this._fadeTime / this.fadeTotalTime) : (this._fadeTime / this.fadeTotalTime)
		}
		else { // Before fade.
			this._fadeProgress = if (isFadeOut) 1.0 else 0.0
		}

		if (this._subFadeState > 0) { // Fade complete event.
			if (!isFadeOut) {
				this._playheadState = this._playheadState or 1 // x1
				this._fadeState = 0
			}

			val eventActive = this._parent === null && this.actionEnabled
			if (eventActive) {
				val eventType = isFadeOut ? EventObject.FADE_OUT_COMPLETE : EventObject.FADE_IN_COMPLETE
				if (this._armature.eventDispatcher.hasDBEventListener(eventType)) {
					val eventObject = BaseObject.borrowObject<EventObject>()
					eventObject.type = eventType
					eventObject.armature = this._armature
					eventObject.animationState = this
					this._armature._dragonBones.bufferEvent(eventObject)
				}
			}
		}
	}
	/**
	 * @internal
	 */
	public fun init(armature: Armature, animationData: AnimationData, animationConfig: AnimationConfig): Unit {
		if (this._armature !== null) {
			return
		}

		this._armature = armature
		this._animationData = animationData
		//
		this.resetToPose = animationConfig.resetToPose
		this.additive = animationConfig.additive
		this.displayControl = animationConfig.displayControl
		this.actionEnabled = animationConfig.actionEnabled
		this.blendType = animationData.blendType
		this.layer = animationConfig.layer
		this.playTimes = animationConfig.playTimes
		this.timeScale = animationConfig.timeScale
		this.fadeTotalTime = animationConfig.fadeInTime
		this.autoFadeOutTime = animationConfig.autoFadeOutTime
		this.name = animationConfig.name.length > 0 ? animationConfig.name : animationConfig.animation
		this.group = animationConfig.group
		//
		this._weight = animationConfig.weight

		if (animationConfig.pauseFadeIn) {
			this._playheadState = 2 // 10
		}
		else {
			this._playheadState = 3 // 11
		}

		if (animationConfig.duration < 0.0) {
			this._position = 0.0
			this._duration = this._animationData.duration

			if (animationConfig.position !== 0.0) {
				if (this.timeScale >= 0.0) {
					this._time = animationConfig.position
				}
				else {
					this._time = animationConfig.position - this._duration
				}
			}
			else {
				this._time = 0.0
			}
		}
		else {
			this._position = animationConfig.position
			this._duration = animationConfig.duration
			this._time = 0.0
		}

		if (this.timeScale < 0.0 && this._time === 0.0) {
			this._time = -0.000001 // Turn to end.
		}

		if (this.fadeTotalTime <= 0.0) {
			this._fadeProgress = 0.999999 // Make different.
		}

		if (animationConfig.boneMask.length > 0) {
			this._boneMask.length = animationConfig.boneMask.length
			for (var i = 0, l = this._boneMask.length; i < l; ++i) {
				this._boneMask[i] = animationConfig.boneMask[i]
			}
		}

		this._actionTimeline = BaseObject.borrowObject<ActionTimelineState>()
		this._actionTimeline.init(this._armature, this, this._animationData.actionTimeline)
		this._actionTimeline.currentTime = this._time

		if (this._actionTimeline.currentTime < 0.0) {
			this._actionTimeline.currentTime = this._duration - this._actionTimeline.currentTime
		}

		if (this._animationData.zOrderTimeline !== null) {
			this._zOrderTimeline = BaseObject.borrowObject<ZOrderTimelineState>()
			this._zOrderTimeline.init(this._armature, this, this._animationData.zOrderTimeline)
		}
	}
	/**
	 * @internal
	 */
	public fun advanceTime(passedTime: Double, cacheFrameRate: Double): Unit {
		// Update fade time.
		if (this._fadeState !== 0 || this._subFadeState !== 0) {
			this._advanceFadeTime(passedTime)
		}
		// Update time.
		if (this._playheadState === 3) { // 11
			if (this.timeScale !== 1.0) {
				passedTime *= this.timeScale
			}

			this._time += passedTime
		}
		// Update timeline.
		if (this._timelineDirty !== 0) {
			if (this._timelineDirty === 2) {
				this._updateTimelines()
			}

			this._timelineDirty = 0
			this._updateBoneAndSlotTimelines()
		}

		val isBlendDirty = this._fadeState !== 0 || this._subFadeState === 0
		val isCacheEnabled = this._fadeState === 0 && cacheFrameRate > 0.0
		var isUpdateTimeline = true
		var isUpdateBoneTimeline = true
		var time = this._time
		this._weightResult = this._weight * this._fadeProgress

		if (this._parent !== null) {
			this._weightResult *= this._parent._weightResult
		}

		if (this._actionTimeline.playState <= 0) { // Update main timeline.
			this._actionTimeline.update(time)
		}

		if (this._weight === 0.0) {
			return
		}

		if (isCacheEnabled) { // Cache time internval.
			val internval = cacheFrameRate * 2.0
			this._actionTimeline.currentTime = Math.floor(this._actionTimeline.currentTime * internval) / internval
		}

		if (this._zOrderTimeline !== null && this._zOrderTimeline.playState <= 0) { // Update zOrder timeline.
			this._zOrderTimeline.update(time)
		}

		if (isCacheEnabled) { // Update cache.
			val cacheFrameIndex = Math.floor(this._actionTimeline.currentTime * cacheFrameRate) // uint
			if (this._armature._cacheFrameIndex === cacheFrameIndex) { // Same cache.
				isUpdateTimeline = false
				isUpdateBoneTimeline = false
			}
			else {
				this._armature._cacheFrameIndex = cacheFrameIndex

				if (this._animationData.cachedFrames[cacheFrameIndex]) { // Cached.
					isUpdateBoneTimeline = false
				}
				else { // Cache.
					this._animationData.cachedFrames[cacheFrameIndex] = true
				}
			}
		}

		if (isUpdateTimeline) {
			var isBlend = false
			var prevTarget: BlendState? = null as any //

			if (isUpdateBoneTimeline) {
				for (var i = 0, l = this._boneTimelines.length; i < l; ++i) {
					val timeline = this._boneTimelines[i]

					if (timeline.playState <= 0) {
						timeline.update(time)
					}

					if (timeline.target !== prevTarget) {
						val blendState = timeline.target as BlendState
						isBlend = blendState.update(this)
						prevTarget = blendState

						if (blendState.dirty === 1) {
							val pose = (blendState.target as Bone).animationPose
							pose.x = 0.0
							pose.y = 0.0
							pose.rotation = 0.0
							pose.skew = 0.0
							pose.scaleX = 1.0
							pose.scaleY = 1.0
						}
					}

					if (isBlend) {
						timeline.blend(isBlendDirty)
					}
				}
			}

			for (var i = 0, l = this._boneBlendTimelines.length; i < l; ++i) {
				val timeline = this._boneBlendTimelines[i]

				if (timeline.playState <= 0) {
					timeline.update(time)
				}

				if ((timeline.target as BlendState).update(this)) {
					timeline.blend(isBlendDirty)
				}
			}

			if (this.displayControl) {
				for (var i = 0, l = this._slotTimelines.length; i < l; ++i) {
					val timeline = this._slotTimelines[i]
					if (timeline.playState <= 0) {
						val slot = timeline.target as Slot
						val displayController = slot.displayController

						if (
							displayController === null ||
							displayController === this.name ||
							displayController === this.group
						) {
							timeline.update(time)
						}
					}
				}
			}

			for (var i = 0, l = this._slotBlendTimelines.length; i < l; ++i) {
				val timeline = this._slotBlendTimelines[i]
				if (timeline.playState <= 0) {
					val blendState = timeline.target as BlendState
					timeline.update(time)

					if (blendState.update(this)) {
						timeline.blend(isBlendDirty)
					}
				}
			}

			for (var i = 0, l = this._constraintTimelines.length; i < l; ++i) {
				val timeline = this._constraintTimelines[i]
				if (timeline.playState <= 0) {
					timeline.update(time)
				}
			}

			if (this._animationTimelines.length > 0) {
				var dL = 100.0
				var dR = 100.0
				var leftState: AnimationState? = null
				var rightState: AnimationState? = null

				for (var i = 0, l = this._animationTimelines.length; i < l; ++i) {
					val timeline = this._animationTimelines[i]
					if (timeline.playState <= 0) {
						timeline.update(time)
					}

					if (this.blendType === AnimationBlendType.E1D) { // TODO
						val animationState = timeline.target as AnimationState
						val d = this.parameterX - animationState.positionX

						if (d >= 0.0) {
							if (d < dL) {
								dL = d
								leftState = animationState
							}
						}
						else {
							if (-d < dR) {
								dR = -d
								rightState = animationState
							}
						}
					}
				}

				if (leftState !== null) {
					if (this._activeChildA !== leftState) {
						if (this._activeChildA !== null) {
							this._activeChildA.weight = 0.0
						}

						this._activeChildA = leftState
						this._activeChildA.activeTimeline()
					}

					if (this._activeChildB !== rightState) {
						if (this._activeChildB !== null) {
							this._activeChildB.weight = 0.0
						}

						this._activeChildB = rightState
					}

					leftState.weight = dR / (dL + dR)

					if (rightState) {
						rightState.weight = 1.0 - leftState.weight
					}
				}
			}
		}

		if (this._fadeState === 0) {
			if (this._subFadeState > 0) {
				this._subFadeState = 0

				if (this._poseTimelines.length > 0) { // Remove pose timelines.
					for (timeline in this._poseTimelines) {
						var index = this._boneTimelines.indexOf(timeline)
						if (index >= 0) {
							this._boneTimelines.splice(index, 1)
							timeline.returnToPool()
							continue
						}

						index = this._boneBlendTimelines.indexOf(timeline)
						if (index >= 0) {
							this._boneBlendTimelines.splice(index, 1)
							timeline.returnToPool()
							continue
						}

						index = this._slotTimelines.indexOf(timeline)
						if (index >= 0) {
							this._slotTimelines.splice(index, 1)
							timeline.returnToPool()
							continue
						}

						index = this._slotBlendTimelines.indexOf(timeline)
						if (index >= 0) {
							this._slotBlendTimelines.splice(index, 1)
							timeline.returnToPool()
							continue
						}

						index = this._constraintTimelines.indexOf(timeline)
						if (index >= 0) {
							this._constraintTimelines.splice(index, 1)
							timeline.returnToPool()
							continue
						}
					}

					this._poseTimelines.length = 0
				}
			}

			if (this._actionTimeline.playState > 0) {
				if (this.autoFadeOutTime >= 0.0) { // Auto fade out.
					this.fadeOut(this.autoFadeOutTime)
				}
			}
		}
	}
	/**
	 * - Continue play.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 继续播放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun play(): Unit {
		this._playheadState = 3 // 11
	}
	/**
	 * - Stop play.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 暂停播放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun stop(): Unit {
		this._playheadState &= 1 // 0x
	}
	/**
	 * - Fade out the animation state.
	 * @param fadeOutTime - The fade out time. (In seconds)
	 * @param pausePlayhead - Whether to pause the animation playing when fade out.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 淡出动画状态。
	 * @param fadeOutTime - 淡出时间。 （以秒为单位）
	 * @param pausePlayhead - 淡出时是否暂停播放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun fadeOut(fadeOutTime: Double, pausePlayhead: Boolean = true): Unit {
		if (fadeOutTime < 0.0) {
			fadeOutTime = 0.0
		}

		if (pausePlayhead) {
			this._playheadState &= 2 // x0
		}

		if (this._fadeState > 0) {
			if (fadeOutTime > this.fadeTotalTime - this._fadeTime) { // If the animation is already in fade out, the new fade out will be ignored.
				return
			}
		}
		else {
			this._fadeState = 1
			this._subFadeState = -1

			if (fadeOutTime <= 0.0 || this._fadeProgress <= 0.0) {
				this._fadeProgress = 0.000001 // Modify fade progress to different value.
			}

			for (timeline in this._boneTimelines) {
				timeline.fadeOut()
			}

			for (timeline in this._boneBlendTimelines) {
				timeline.fadeOut()
			}

			for (timeline in this._slotTimelines) {
				timeline.fadeOut()
			}

			for (timeline in this._slotBlendTimelines) {
				timeline.fadeOut()
			}

			for (timeline in this._constraintTimelines) {
				timeline.fadeOut()
			}

			for (timeline in this._animationTimelines) {
				timeline.fadeOut()
				//
				val animaitonState = timeline.target as AnimationState
				animaitonState.fadeOut(999999.0, true)
			}
		}

		this.displayControl = false //
		this.fadeTotalTime = this._fadeProgress > 0.000001 ? fadeOutTime / this._fadeProgress : 0.0
		this._fadeTime = this.fadeTotalTime * (1.0 - this._fadeProgress)
	}
	/**
	 * - Check if a specific bone mask is included.
	 * @param boneName - The bone name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 检查是否包含特定骨骼遮罩。
	 * @param boneName - 骨骼名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun containsBoneMask(boneName: String): Boolean {
		return this._boneMask.length === 0 || this._boneMask.indexOf(boneName) >= 0
	}
	/**
	 * - Add a specific bone mask.
	 * @param boneName - The bone name.
	 * @param recursive - Whether or not to add a mask to the bone's sub-bone.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 添加特定的骨骼遮罩。
	 * @param boneName - 骨骼名称。
	 * @param recursive - 是否为该骨骼的子骨骼添加遮罩。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun addBoneMask(boneName: String, recursive: Boolean = true): Unit {
		val currentBone = this._armature.getBone(boneName)
		if (currentBone === null) {
			return
		}

		if (this._boneMask.indexOf(boneName) < 0) { // Add mixing
			this._boneMask.push(boneName)
		}

		if (recursive) { // Add recursive mixing.
			for (bone in this._armature.getBones()) {
				if (this._boneMask.indexOf(bone.name) < 0 && currentBone.contains(bone)) {
					this._boneMask.push(bone.name)
				}
			}
		}

		this._timelineDirty = 1
	}
	/**
	 * - Remove the mask of a specific bone.
	 * @param boneName - The bone name.
	 * @param recursive - Whether to remove the bone's sub-bone mask.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 删除特定骨骼的遮罩。
	 * @param boneName - 骨骼名称。
	 * @param recursive - 是否删除该骨骼的子骨骼遮罩。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun removeBoneMask(boneName: String, recursive: Boolean = true): Unit {
		val index = this._boneMask.indexOf(boneName)
		if (index >= 0) { // Remove mixing.
			this._boneMask.splice(index, 1)
		}

		if (recursive) {
			val currentBone = this._armature.getBone(boneName)
			if (currentBone !== null) {
				val bones = this._armature.getBones()
				if (this._boneMask.length > 0) { // Remove recursive mixing.
					for (bone in bones) {
						val index = this._boneMask.indexOf(bone.name)
						if (index >= 0 && currentBone.contains(bone)) {
							this._boneMask.splice(index, 1)
						}
					}
				}
				else { // Add unrecursive mixing.
					for (bone in bones) {
						if (bone === currentBone) {
							continue
						}

						if (!currentBone.contains(bone)) {
							this._boneMask.push(bone.name)
						}
					}
				}
			}
		}

		this._timelineDirty = 1
	}
	/**
	 * - Remove all bone masks.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 删除所有骨骼遮罩。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public fun removeAllBoneMask(): Unit {
		this._boneMask.length = 0
		this._timelineDirty = 1
	}
	/**
	 * @private
	 */
	public fun addState(animationState: AnimationState, timelineDatas: TimelineData[]? = null) {
		if (timelineDatas !== null) {
			for (timelineData in timelineDatas) {
				switch (timelineData.type) {
					case TimelineType.AnimationProgress: {
						val timeline = BaseObject.borrowObject<AnimationProgressTimelineState>()
						timeline.target = animationState
						timeline.init(this._armature, this, timelineData)
						this._animationTimelines.push(timeline)

						if (this.blendType !== AnimationBlendType.None) {
							val animaitonTimelineData = timelineData as AnimationTimelineData
							animationState.positionX = animaitonTimelineData.x
							animationState.positionY = animaitonTimelineData.y
							animationState.weight = 0.0
						}

						animationState._parent = this
						this.resetToPose = false
						break
					}

					case TimelineType.AnimationWeight: {
						val timeline = BaseObject.borrowObject<AnimationWeightTimelineState>()
						timeline.target = animationState
						timeline.init(this._armature, this, timelineData)
						this._animationTimelines.push(timeline)
						break
					}

					case TimelineType.AnimationParameter: {
						val timeline = BaseObject.borrowObject<AnimationParametersTimelineState>()
						timeline.target = animationState
						timeline.init(this._armature, this, timelineData)
						this._animationTimelines.push(timeline)
						break
					}

					default:
						break
				}
			}
		}

		if (animationState._parent === null) {
			animationState._parent = this
		}
	}
	/**
	 * @internal
	 */
	public fun activeTimeline(): Unit {
		for (timeline in this._slotTimelines) {
			timeline.dirty = true
			timeline.currentTime = -1.0
		}
	}
	/**
	 * - Whether the animation state is fading in.
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 是否正在淡入。
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	public val isFadeIn: Boolean get() {
		return this._fadeState < 0
	}
	/**
	 * - Whether the animation state is fading out.
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 是否正在淡出。
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	public val isFadeOut: Boolean get() {
		return this._fadeState > 0
	}
	/**
	 * - Whether the animation state is fade completed.
	 * @version DragonBones 5.1
	 * @language en_US
	 */
	/**
	 * - 是否淡入或淡出完毕。
	 * @version DragonBones 5.1
	 * @language zh_CN
	 */
	public val isFadeComplete: Boolean get() {
		return this._fadeState === 0
	}
	/**
	 * - Whether the animation state is playing.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 是否正在播放。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public val isPlaying: Boolean get() {
		return (this._playheadState & 2) !== 0 && this._actionTimeline.playState <= 0
	}
	/**
	 * - Whether the animation state is play completed.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 是否播放完毕。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public val isCompleted: Boolean get() {
		return this._actionTimeline.playState > 0
	}
	/**
	 * - The times has been played.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 已经循环播放的次数。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public val currentPlayTimes: Double get() {
		return this._actionTimeline.currentPlayTimes
	}
	/**
	 * - The total time. (In seconds)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 总播放时间。 （以秒为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public val totalTime: Double get() {
		return this._duration
	}
	/**
	 * - The time is currently playing. (In seconds)
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 当前播放的时间。 （以秒为单位）
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var currentTime: Double
		get() {
		return this._actionTimeline.currentTime
	}
	set(value: Double) {
		val currentPlayTimes = this._actionTimeline.currentPlayTimes - (this._actionTimeline.playState > 0 ? 1 : 0)
		if (value < 0 || this._duration < value) {
			value = (value % this._duration) + currentPlayTimes * this._duration
			if (value < 0) {
				value += this._duration
			}
		}

		if (
			this.playTimes > 0 && currentPlayTimes === this.playTimes - 1 &&
			value === this._duration && this._parent === null
		) {
			value = this._duration - 0.000001 //
		}

		if (this._time === value) {
			return
		}

		this._time = value
		this._actionTimeline.setCurrentTime(this._time)

		if (this._zOrderTimeline !== null) {
			this._zOrderTimeline.playState = -1
		}

		for (timeline in this._boneTimelines) {
			timeline.playState = -1
		}

		for (timeline in this._slotTimelines) {
			timeline.playState = -1
		}
	}
	/**
	 * - The blend weight.
	 * @default 1.0
	 * @version DragonBones 5.0
	 * @language en_US
	 */
	/**
	 * - 混合权重。
	 * @default 1.0
	 * @version DragonBones 5.0
	 * @language zh_CN
	 */
	/**
	 * - The animation data.
	 * @see dragonBones.AnimationData
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	public var weight: Double get() {
		return this._weight
	}
	set(value: Double) {
		if (this._weight === value) {
			return
		}

		this._weight = value

		for (timeline in this._boneTimelines) {
			timeline.dirty = true
		}

		for (timeline in this._boneBlendTimelines) {
			timeline.dirty = true
		}

		for (timeline in this._slotBlendTimelines) {
			timeline.dirty = true
		}
	}
	/**
	 * - 动画数据。
	 * @see dragonBones.AnimationData
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public val animationData: AnimationData get() {
		return this._animationData
	}
}
/**
 * @internal
 */
class BlendState  :  BaseObject() {
	companion object {
		const val BONE_TRANSFORM: String = "boneTransform"
		const val BONE_ALPHA: String = "boneAlpha"
		const val SURFACE: String = "surface"
		const val SLOT_DEFORM: String = "slotDeform"
		const val SLOT_ALPHA: String = "slotAlpha"
		const val SLOT_Z_INDEX: String = "slotZIndex"
	}

	public override fun toString(): String {
		return "[class dragonBones.BlendState]"
	}

	public var dirty: Double
	public var layer: Double
	public var leftWeight: Double
	public var layerWeight: Double
	public var blendWeight: Double
	public var target: BaseObject

	protected fun _onClear(): Unit {
		this.reset()

		this.target = null as any
	}

	public fun update(animationState: AnimationState): Boolean {
		val animationLayer = animationState.layer
		var animationWeight = animationState._weightResult

		if (this.dirty > 0) {
			if (this.leftWeight > 0.0) {
				if (this.layer !== animationLayer) {
					if (this.layerWeight >= this.leftWeight) {
						this.dirty++
						this.layer = animationLayer
						this.leftWeight = 0.0
						this.blendWeight = 0.0

						return false
					}

					this.layer = animationLayer
					this.leftWeight -= this.layerWeight
					this.layerWeight = 0.0
				}

				animationWeight *= this.leftWeight
				this.dirty++
				this.blendWeight = animationWeight
				this.layerWeight += this.blendWeight

				return true
			}

			return false
		}

		this.dirty++
		this.layer = animationLayer
		this.leftWeight = 1.0
		this.blendWeight = animationWeight
		this.layerWeight = animationWeight

		return true
	}

	public fun reset(): Unit {
		this.dirty = 0
		this.layer = 0
		this.leftWeight = 0.0
		this.layerWeight = 0.0
		this.blendWeight = 0.0
	}
}
