package com.dragonbones.core

import com.dragonbones.animation.*
import com.dragonbones.event.*

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

/**
 * @private
 */
enum class BinaryOffset(val index: Int) {
	WeigthBoneCount(0),
	WeigthFloatOffset(1),
	WeigthBoneIndices(2),

	GeometryVertexCount(0),
	GeometryTriangleCount(1),
	GeometryFloatOffset(2),
	GeometryWeightOffset(3),
	GeometryVertexIndices(4),

	TimelineScale(0),
	TimelineOffset(1),
	TimelineKeyFrameCount(2),
	TimelineFrameValueCount(3),
	TimelineFrameValueOffset(4),
	TimelineFrameOffset(5),

	FramePosition(0),
	FrameTweenType(1),
	FrameTweenEasingOrCurveSampleCount(2),
	FrameCurveSamples(3),

	DeformVertexOffset(0),
	DeformCount(1),
	DeformValueCount(2),
	DeformValueOffset(3),
	DeformFloatOffset(4)
}
/**
 * @private
 */
enum class ArmatureType(val id: Int) {
	Armature(0),
	MovieClip(1),
	Stage(2)
}
/**
 * @private
 */
enum class BoneType(val id: Int) {
	Bone(0),
	Surface(1)
}
/**
 * @private
 */
enum class DisplayType(val id: Int) {
	Image(0),
	Armature(1),
	Mesh(2),
	BoundingBox(3),
	Path(4)
}
/**
 * - Bounding box type.
 * @version DragonBones 5.0
 * @language en_US
 */
/**
 * - 边界框类型。
 * @version DragonBones 5.0
 * @language zh_CN
 */
enum class BoundingBoxType(val id: Int) {
	None(-1),
	Rectangle(0),
	Ellipse(1),
	Polygon(2)
}
/**
 * @private
 */
enum class ActionType(val id: Int) {
	Play(0),
	Frame(10),
	Sound(11)
}
/**
 * @private
 */
enum class BlendMode(val id: Int) {
	Normal(0),
	Add(1),
	Alpha(2),
	Darken(3),
	Difference(4),
	Erase(5),
	HardLight(6),
	Invert(7),
	Layer(8),
	Lighten(9),
	Multiply(10),
	Overlay(11),
	Screen(12),
	Subtract(13)
}
/**
 * @private
 */
enum class TweenType(val id: Int) {
	None(0),
	Line(1),
	Curve(2),
	QuadIn(3),
	QuadOut(4),
	QuadInOut(5)
}
/**
 * @private
 */
enum class TimelineType(val id: Int) {
	Action(0),
	ZOrder(1),

	BoneAll(10),
	BoneTranslate(11),
	BoneRotate(12),
	BoneScale(13),

	Surface(50),
	BoneAlpha(60),

	SlotDisplay(20),
	SlotColor(21),
	SlotDeform(22),
	SlotZIndex(23),
	SlotAlpha(24),

	IKConstraint(30),

	AnimationProgress(40),
	AnimationWeight(41),
	AnimationParameter(42),
}
/**
 * - Offset mode.
 * @version DragonBones 5.5
 * @language en_US
 */
/**
 * - 偏移模式。
 * @version DragonBones 5.5
 * @language zh_CN
 */
enum class OffsetMode {
	None,
	Additive,
	Override,
}
/**
 * - Animation fade out mode.
 * @version DragonBones 4.5
 * @language en_US
 */
/**
 * - 动画淡出模式。
 * @version DragonBones 4.5
 * @language zh_CN
 */
enum class AnimationFadeOutMode(val id: Int) {
	/**
	 * - Fade out the animation states of the same layer.
	 * @language en_US
	 */
	/**
	 * - 淡出同层的动画状态。
	 * @language zh_CN
	 */
	SameLayer(1),
	/**
	 * - Fade out the animation states of the same group.
	 * @language en_US
	 */
	/**
	 * - 淡出同组的动画状态。
	 * @language zh_CN
	 */
	SameGroup(2),
	/**
	 * - Fade out the animation states of the same layer and group.
	 * @language en_US
	 */
	/**
	 * - 淡出同层并且同组的动画状态。
	 * @language zh_CN
	 */
	SameLayerAndGroup(3),
	/**
	 * - Fade out of all animation states.
	 * @language en_US
	 */
	/**
	 * - 淡出所有的动画状态。
	 * @language zh_CN
	 */
	All(4),
	/**
	 * - Does not replace the animation state with the same name.
	 * @language en_US
	 */
	/**
	 * - 不替换同名的动画状态。
	 * @language zh_CN
	 */
	Single(5),
}
/**
 * @private
 */
enum class AnimationBlendType {
	None,
	E1D,
}
/**
 * @private
 */
enum class AnimationBlendMode {
	Additive,
	Override,
}
/**
 * @private
 */
enum class ConstraintType {
	IK,
	Path
}
/**
 * @private
 */
enum class PositionMode {
	Fixed,
	Percent
}
/**
 * @private
 */
enum class SpacingMode {
	Length,
	Fixed,
	Percent
}
/**
 * @private
 */
enum class RotateMode {
	Tangent,
	Chain,
	ChainScale
}
/**
 * @private
 */
//interface Map<T> {
//	[key: String]: T
//}
/**
 * @private
 */
class DragonBones {
	companion object {
		public val VERSION: String = "5.7.000"

		public var yDown: Boolean = true
		public var debug: Boolean = false
		public var debugDraw: Boolean = false
	}

	private val _clock: WorldClock = WorldClock()
	private val _events: ArrayList<EventObject> = arrayListOf()
	private val _objects: ArrayList<BaseObject> = arrayListOf()
	private var _eventManager: IEventDispatcher = null as any

	public constructor(eventManager: IEventDispatcher) {
		this._eventManager = eventManager

		println("DragonBones: ${DragonBones.VERSION}\nWebsite: http://dragonbones.com/\nSource and Demo: https://github.com/DragonBones/")
	}

	public fun advanceTime(passedTime: Double): Unit {
		if (this._objects.size > 0) {
			for (object in this._objects) {
				object.returnToPool()
			}

			this._objects.clear()
		}

		this._clock.advanceTime(passedTime)

		if (this._events.size > 0) {
			for (i in 0 until this._events.size) {
				val eventObject = this._events[i]
				val armature = eventObject.armature

				if (armature._armatureData != null) { // May be armature disposed before advanceTime.
					armature.eventDispatcher.dispatchDBEvent(eventObject.type, eventObject)
					if (eventObject.type === EventObject.SOUND_EVENT) {
						this._eventManager.dispatchDBEvent(eventObject.type, eventObject)
					}
				}

				this.bufferObject(eventObject)
			}

			this._events.size = 0
		}
	}

	public fun bufferEvent(value: EventObject): Unit {
		if (this._events.indexOf(value) < 0) {
			this._events.add(value)
		}
	}

	public fun bufferObject(obj: BaseObject): Unit {
		if (this._objects.indexOf(obj) < 0) {
			this._objects.add(obj)
		}
	}

	public val clock: WorldClock
		get() {
		return this._clock
		}

	public val eventManager: IEventDispatcher
		get() {
		return this._eventManager
		}
}

/*
//
if (!console.warn) {
    console.warn = function () { };
}

if (!console.assert) {
    console.assert = function () { };
}
//
if (!Date.now) {
    Date.now = function now() {
        return new Date().getTime();
    };
}
// Weixin can not support typescript  : .
var __extends: Any = function (t: Any, e: Any) {
    function r(this: Any) {
        this.constructor = t;
    }
    for (var i in e) {
        if ((e as any).hasOwnProperty(i)) {
            t[i] = e[i];
        }
    }
    r.prototype = e.prototype, t.prototype = new (r as any)();
};
//
if (typeof global === "undefined" && typeof window !== "undefined") {
    var global = window as any;
}
//
declare var exports: Any;
declare var module: Any;
declare var define: Any;
if (typeof exports === "object" && typeof module === "object") {
    module.exports = dragonBones;
}
else if (typeof define === "function" && define["amd"]) {
    define(["dragonBones"], function () { return dragonBones; });
}
else if (typeof exports === "object") {
    exports = dragonBones;
}
else if (typeof global !== "undefined") {
    global.dragonBones = dragonBones;
}
*/