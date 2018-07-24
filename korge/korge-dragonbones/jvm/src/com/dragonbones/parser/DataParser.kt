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
		val val DATA_VERSION_2_3: String = "2.3"
		val val DATA_VERSION_3_0: String = "3.0"
		val val DATA_VERSION_4_0: String = "4.0"
		val val DATA_VERSION_4_5: String = "4.5"
		val val DATA_VERSION_5_0: String = "5.0"
		val val DATA_VERSION_5_5: String = "5.5"
		val val DATA_VERSION_5_6: String = "5.6"
		val DATA_VERSION: String = DataParser.DATA_VERSION_5_6

		val DATA_VERSIONS: Array<String> = arrayOf(
			DataParser.DATA_VERSION_4_0,
			DataParser.DATA_VERSION_4_5,
			DataParser.DATA_VERSION_5_0,
			DataParser.DATA_VERSION_5_5,
			DataParser.DATA_VERSION_5_6
		)

		val val TEXTURE_ATLAS: String = "textureAtlas"
		val val SUB_TEXTURE: String = "SubTexture"
		val val FORMAT: String = "format"
		val val IMAGE_PATH: String = "imagePath"
		val val WIDTH: String = "width"
		val val HEIGHT: String = "height"
		val val ROTATED: String = "rotated"
		val val FRAME_X: String = "frameX"
		val val FRAME_Y: String = "frameY"
		val val FRAME_WIDTH: String = "frameWidth"
		val val FRAME_HEIGHT: String = "frameHeight"

		val val DRADON_BONES: String = "dragonBones"
		val val USER_DATA: String = "userData"
		val val ARMATURE: String = "armature"
		val val CANVAS: String = "canvas"
		val val BONE: String = "bone"
		val val SURFACE: String = "surface"
		val val SLOT: String = "slot"
		val val CONSTRAINT: String = "constraint"
		val val SKIN: String = "skin"
		val val DISPLAY: String = "display"
		val val FRAME: String = "frame"
		val val IK: String = "ik"
		val val PATH_CONSTRAINT: String = "path"

		val val ANIMATION: String = "animation"
		val val TIMELINE: String = "timeline"
		val val FFD: String = "ffd"
		val val TRANSLATE_FRAME: String = "translateFrame"
		val val ROTATE_FRAME: String = "rotateFrame"
		val val SCALE_FRAME: String = "scaleFrame"
		val val DISPLAY_FRAME: String = "displayFrame"
		val val COLOR_FRAME: String = "colorFrame"
		val val DEFAULT_ACTIONS: String = "defaultActions"
		val val ACTIONS: String = "actions"
		val val EVENTS: String = "events"

		val val INTS: String = "ints"
		val val FLOATS: String = "floats"
		val val STRINGS: String = "strings"

		val val TRANSFORM: String = "transform"
		val val PIVOT: String = "pivot"
		val val AABB: String = "aabb"
		val val COLOR: String = "color"

		val val VERSION: String = "version"
		val val COMPATIBLE_VERSION: String = "compatibleVersion"
		val val FRAME_RATE: String = "frameRate"
		val val TYPE: String = "type"
		val val SUB_TYPE: String = "subType"
		val val NAME: String = "name"
		val val PARENT: String = "parent"
		val val TARGET: String = "target"
		val val STAGE: String = "stage"
		val val SHARE: String = "share"
		val val PATH: String = "path"
		val val LENGTH: String = "length"
		val val DISPLAY_INDEX: String = "displayIndex"
		val val Z_ORDER: String = "zOrder"
		val val Z_INDEX: String = "zIndex"
		val val BLEND_MODE: String = "blendMode"
		val val INHERIT_TRANSLATION: String = "inheritTranslation"
		val val INHERIT_ROTATION: String = "inheritRotation"
		val val INHERIT_SCALE: String = "inheritScale"
		val val INHERIT_REFLECTION: String = "inheritReflection"
		val val INHERIT_ANIMATION: String = "inheritAnimation"
		val val INHERIT_DEFORM: String = "inheritDeform"
		val val SEGMENT_X: String = "segmentX"
		val val SEGMENT_Y: String = "segmentY"
		val val BEND_POSITIVE: String = "bendPositive"
		val val CHAIN: String = "chain"
		val val WEIGHT: String = "weight"

		val val BLEND_TYPE: String = "blendType"
		val val FADE_IN_TIME: String = "fadeInTime"
		val val PLAY_TIMES: String = "playTimes"
		val val SCALE: String = "scale"
		val val OFFSET: String = "offset"
		val val POSITION: String = "position"
		val val DURATION: String = "duration"
		val val TWEEN_EASING: String = "tweenEasing"
		val val TWEEN_ROTATE: String = "tweenRotate"
		val val TWEEN_SCALE: String = "tweenScale"
		val val CLOCK_WISE: String = "clockwise"
		val val CURVE: String = "curve"
		val val SOUND: String = "sound"
		val val EVENT: String = "event"
		val val ACTION: String = "action"

		val val X: String = "x"
		val val Y: String = "y"
		val val SKEW_X: String = "skX"
		val val SKEW_Y: String = "skY"
		val val SCALE_X: String = "scX"
		val val SCALE_Y: String = "scY"
		val val VALUE: String = "value"
		val val ROTATE: String = "rotate"
		val val SKEW: String = "skew"
		val val ALPHA: String = "alpha"

		val val ALPHA_OFFSET: String = "aO"
		val val RED_OFFSET: String = "rO"
		val val GREEN_OFFSET: String = "gO"
		val val BLUE_OFFSET: String = "bO"
		val val ALPHA_MULTIPLIER: String = "aM"
		val val RED_MULTIPLIER: String = "rM"
		val val GREEN_MULTIPLIER: String = "gM"
		val val BLUE_MULTIPLIER: String = "bM"

		val val UVS: String = "uvs"
		val val VERTICES: String = "vertices"
		val val TRIANGLES: String = "triangles"
		val val WEIGHTS: String = "weights"
		val val SLOT_POSE: String = "slotPose"
		val val BONE_POSE: String = "bonePose"

		val val BONES: String = "bones"
		val val POSITION_MODE: String = "positionMode"
		val val SPACING_MODE: String = "spacingMode"
		val val ROTATE_MODE: String = "rotateMode"
		val val SPACING: String = "spacing"
		val val ROTATE_OFFSET: String = "rotateOffset"
		val val ROTATE_MIX: String = "rotateMix"
		val val TRANSLATE_MIX: String = "translateMix"

		val val TARGET_DISPLAY: String = "targetDisplay"
		val val CLOSED: String = "closed"
		val val CONSTANT_SPEED: String = "constantSpeed"
		val val VERTEX_COUNT: String = "vertexCount"
		val val LENGTHS: String = "lengths"

		val val GOTO_AND_PLAY: String = "gotoAndPlay"

		val val DEFAULT_NAME: String = "default"

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
