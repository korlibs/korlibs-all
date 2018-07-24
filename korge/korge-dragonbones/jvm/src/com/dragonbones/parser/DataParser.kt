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
package com.dragonbones.parser

import com.dragonbones.core.*
import com.dragonbones.model.*

/**
 * @private
 */
@Suppress("unused")
abstract class DataParser {
	companion object {
		const val DATA_VERSION_2_3: String = "2.3"
		const val DATA_VERSION_3_0: String = "3.0"
		const val DATA_VERSION_4_0: String = "4.0"
		const val DATA_VERSION_4_5: String = "4.5"
		const val DATA_VERSION_5_0: String = "5.0"
		const val DATA_VERSION_5_5: String = "5.5"
		const val DATA_VERSION_5_6: String = "5.6"
		val DATA_VERSION: String = DataParser.DATA_VERSION_5_6

		val DATA_VERSIONS: Array<String> = arrayOf(
			DataParser.DATA_VERSION_4_0,
			DataParser.DATA_VERSION_4_5,
			DataParser.DATA_VERSION_5_0,
			DataParser.DATA_VERSION_5_5,
			DataParser.DATA_VERSION_5_6
		)

		const val TEXTURE_ATLAS: String = "textureAtlas"
		const val SUB_TEXTURE: String = "SubTexture"
		const val FORMAT: String = "format"
		const val IMAGE_PATH: String = "imagePath"
		const val WIDTH: String = "width"
		const val HEIGHT: String = "height"
		const val ROTATED: String = "rotated"
		const val FRAME_X: String = "frameX"
		const val FRAME_Y: String = "frameY"
		const val FRAME_WIDTH: String = "frameWidth"
		const val FRAME_HEIGHT: String = "frameHeight"

		const val DRADON_BONES: String = "dragonBones"
		const val USER_DATA: String = "userData"
		const val ARMATURE: String = "armature"
		const val CANVAS: String = "canvas"
		const val BONE: String = "bone"
		const val SURFACE: String = "surface"
		const val SLOT: String = "slot"
		const val CONSTRAINT: String = "constraint"
		const val SKIN: String = "skin"
		const val DISPLAY: String = "display"
		const val FRAME: String = "frame"
		const val IK: String = "ik"
		const val PATH_CONSTRAINT: String = "path"

		const val ANIMATION: String = "animation"
		const val TIMELINE: String = "timeline"
		const val FFD: String = "ffd"
		const val TRANSLATE_FRAME: String = "translateFrame"
		const val ROTATE_FRAME: String = "rotateFrame"
		const val SCALE_FRAME: String = "scaleFrame"
		const val DISPLAY_FRAME: String = "displayFrame"
		const val COLOR_FRAME: String = "colorFrame"
		const val DEFAULT_ACTIONS: String = "defaultActions"
		const val ACTIONS: String = "actions"
		const val EVENTS: String = "events"

		const val INTS: String = "ints"
		const val FLOATS: String = "floats"
		const val STRINGS: String = "strings"

		const val TRANSFORM: String = "transform"
		const val PIVOT: String = "pivot"
		const val AABB: String = "aabb"
		const val COLOR: String = "color"

		const val VERSION: String = "version"
		const val COMPATIBLE_VERSION: String = "compatibleVersion"
		const val FRAME_RATE: String = "frameRate"
		const val TYPE: String = "type"
		const val SUB_TYPE: String = "subType"
		const val NAME: String = "name"
		const val PARENT: String = "parent"
		const val TARGET: String = "target"
		const val STAGE: String = "stage"
		const val SHARE: String = "share"
		const val PATH: String = "path"
		const val LENGTH: String = "length"
		const val DISPLAY_INDEX: String = "displayIndex"
		const val Z_ORDER: String = "zOrder"
		const val Z_INDEX: String = "zIndex"
		const val BLEND_MODE: String = "blendMode"
		const val INHERIT_TRANSLATION: String = "inheritTranslation"
		const val INHERIT_ROTATION: String = "inheritRotation"
		const val INHERIT_SCALE: String = "inheritScale"
		const val INHERIT_REFLECTION: String = "inheritReflection"
		const val INHERIT_ANIMATION: String = "inheritAnimation"
		const val INHERIT_DEFORM: String = "inheritDeform"
		const val SEGMENT_X: String = "segmentX"
		const val SEGMENT_Y: String = "segmentY"
		const val BEND_POSITIVE: String = "bendPositive"
		const val CHAIN: String = "chain"
		const val WEIGHT: String = "weight"

		const val BLEND_TYPE: String = "blendType"
		const val FADE_IN_TIME: String = "fadeInTime"
		const val PLAY_TIMES: String = "playTimes"
		const val SCALE: String = "scale"
		const val OFFSET: String = "offset"
		const val POSITION: String = "position"
		const val DURATION: String = "duration"
		const val TWEEN_EASING: String = "tweenEasing"
		const val TWEEN_ROTATE: String = "tweenRotate"
		const val TWEEN_SCALE: String = "tweenScale"
		const val CLOCK_WISE: String = "clockwise"
		const val CURVE: String = "curve"
		const val SOUND: String = "sound"
		const val EVENT: String = "event"
		const val ACTION: String = "action"

		const val X: String = "x"
		const val Y: String = "y"
		const val SKEW_X: String = "skX"
		const val SKEW_Y: String = "skY"
		const val SCALE_X: String = "scX"
		const val SCALE_Y: String = "scY"
		const val VALUE: String = "value"
		const val ROTATE: String = "rotate"
		const val SKEW: String = "skew"
		const val ALPHA: String = "alpha"

		const val ALPHA_OFFSET: String = "aO"
		const val RED_OFFSET: String = "rO"
		const val GREEN_OFFSET: String = "gO"
		const val BLUE_OFFSET: String = "bO"
		const val ALPHA_MULTIPLIER: String = "aM"
		const val RED_MULTIPLIER: String = "rM"
		const val GREEN_MULTIPLIER: String = "gM"
		const val BLUE_MULTIPLIER: String = "bM"

		const val UVS: String = "uvs"
		const val VERTICES: String = "vertices"
		const val TRIANGLES: String = "triangles"
		const val WEIGHTS: String = "weights"
		const val SLOT_POSE: String = "slotPose"
		const val BONE_POSE: String = "bonePose"

		const val BONES: String = "bones"
		const val POSITION_MODE: String = "positionMode"
		const val SPACING_MODE: String = "spacingMode"
		const val ROTATE_MODE: String = "rotateMode"
		const val SPACING: String = "spacing"
		const val ROTATE_OFFSET: String = "rotateOffset"
		const val ROTATE_MIX: String = "rotateMix"
		const val TRANSLATE_MIX: String = "translateMix"

		const val TARGET_DISPLAY: String = "targetDisplay"
		const val CLOSED: String = "closed"
		const val CONSTANT_SPEED: String = "constantSpeed"
		const val VERTEX_COUNT: String = "vertexCount"
		const val LENGTHS: String = "lengths"

		const val GOTO_AND_PLAY: String = "gotoAndPlay"

		const val DEFAULT_NAME: String = "default"

		fun _getArmatureType(value: String): ArmatureType {
			return when (value.toLowerCase()) {
				"stage" -> ArmatureType.Stage
				"armature" -> ArmatureType.Armature
				"movieclip" -> ArmatureType.MovieClip
				else -> ArmatureType.Armature
			}
		}

		fun _getBoneType(value: String): BoneType {
			return when (value.toLowerCase()) {
				"bone" -> BoneType.Bone
				"surface" -> BoneType.Surface
				else -> BoneType.Bone
			}
		}

		fun _getPositionMode(value: String): PositionMode {
			return when (value.toLowerCase()) {
				"percent" -> PositionMode.Percent
				"fixed" -> PositionMode.Fixed
				else -> PositionMode.Percent
			}
		}

		fun _getSpacingMode(value: String): SpacingMode {
			return when (value.toLowerCase()) {
				"length" -> SpacingMode.Length
				"percent" -> SpacingMode.Percent
				"fixed" -> SpacingMode.Fixed
				else -> SpacingMode.Length
			}
		}

		fun _getRotateMode(value: String): RotateMode {
			return when (value.toLowerCase()) {
				"tangent" -> RotateMode.Tangent
				"chain" -> RotateMode.Chain
				"chainscale" -> RotateMode.ChainScale
				else -> RotateMode.Tangent
			}
		}

		fun _getDisplayType(value: String): DisplayType {
			return when (value.toLowerCase()) {
				"image" -> DisplayType.Image
				"mesh" -> DisplayType.Mesh
				"armature" -> DisplayType.Armature
				"boundingbox" -> DisplayType.BoundingBox
				"path" -> DisplayType.Path
				else -> DisplayType.Image
			}
		}

		fun _getBoundingBoxType(value: String): BoundingBoxType {
			return when (value.toLowerCase()) {
				"rectangle" -> BoundingBoxType.Rectangle
				"ellipse" -> BoundingBoxType.Ellipse
				"polygon" -> BoundingBoxType.Polygon
				else -> BoundingBoxType.Rectangle
			}
		}

		fun _getBlendMode(value: String): BlendMode {
			when (value.toLowerCase()) {
				"normal" -> return BlendMode.Normal
				"add" -> return BlendMode.Add
				"alpha" -> return BlendMode.Alpha
				"darken" -> return BlendMode.Darken
				"difference" -> return BlendMode.Difference
				"erase" -> return BlendMode.Erase
				"hardlight" -> return BlendMode.HardLight
				"invert" -> return BlendMode.Invert
				"layer" -> return BlendMode.Layer
				"lighten" -> return BlendMode.Lighten
				"multiply" -> return BlendMode.Multiply
				"overlay" -> return BlendMode.Overlay
				"screen" -> return BlendMode.Screen
				"subtract" -> return BlendMode.Subtract
				else -> return BlendMode.Normal
			}
		}

		fun _getAnimationBlendType(value: String): AnimationBlendType {
			when (value.toLowerCase()) {
				"none" -> return AnimationBlendType.None
				"1d" -> return AnimationBlendType.E1D
				else -> return AnimationBlendType.None
			}
		}

		fun _getActionType(value: String): ActionType {
			when (value.toLowerCase()) {
				"play" -> return ActionType.Play
				"frame" -> return ActionType.Frame
				"sound" -> return ActionType.Sound
				else -> return ActionType.Play
			}
		}
	}

	abstract fun parseDragonBonesData(rawData: Any, scale: Double): DragonBonesData?
	abstract fun parseTextureAtlasData(rawData: Any, textureAtlasData: TextureAtlasData, scale: Double): Boolean
}
