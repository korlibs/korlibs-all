package com.dragonbones.parser

import com.dragonbones.core.*
import com.dragonbones.factory.BaseFactory
import com.dragonbones.geom.Rectangle
import com.dragonbones.model.DragonBonesData
import com.dragonbones.model.TextureAtlasData
import com.dragonbones.util.ArrayBase
import com.dragonbones.util.Console
import com.dragonbones.util.buffer.ArrayBuffer
import com.dragonbones.util.json.JSON

import java.util.HashMap

import com.dragonbones.util.Dynamic.*

/**
 * @private
 */
abstract class DataParser {

    /**
     * @private
     */
    abstract fun parseDragonBonesData(rawData: Any, scale: Float): DragonBonesData?

    /**
     * @private
     */
    abstract fun parseTextureAtlasData(rawData: Any, textureAtlasData: TextureAtlasData, scale: Float): Boolean

    companion object {
        protected val DATA_VERSION_2_3 = "2.3"
        protected val DATA_VERSION_3_0 = "3.0"
        protected val DATA_VERSION_4_0 = "4.0"
        protected val DATA_VERSION_4_5 = "4.5"
        protected val DATA_VERSION_5_0 = "5.0"
        protected val DATA_VERSION = DataParser.DATA_VERSION_5_0

        protected val DATA_VERSIONS =
            arrayOf(DataParser.DATA_VERSION_4_0, DataParser.DATA_VERSION_4_5, DataParser.DATA_VERSION_5_0)

        protected val TEXTURE_ATLAS = "textureAtlas"
        protected val SUB_TEXTURE = "SubTexture"
        protected val FORMAT = "format"
        protected val IMAGE_PATH = "imagePath"
        protected val WIDTH = "width"
        protected val HEIGHT = "height"
        protected val ROTATED = "rotated"
        protected val FRAME_X = "frameX"
        protected val FRAME_Y = "frameY"
        protected val FRAME_WIDTH = "frameWidth"
        protected val FRAME_HEIGHT = "frameHeight"

        protected val DRADON_BONES = "dragonBones"
        protected val USER_DATA = "userData"
        protected val ARMATURE = "armature"
        protected val BONE = "bone"
        protected val IK = "ik"
        protected val SLOT = "slot"
        protected val SKIN = "skin"
        protected val DISPLAY = "display"
        protected val ANIMATION = "animation"
        protected val Z_ORDER = "zOrder"
        protected val FFD = "ffd"
        protected val FRAME = "frame"
        protected val TRANSLATE_FRAME = "translateFrame"
        protected val ROTATE_FRAME = "rotateFrame"
        protected val SCALE_FRAME = "scaleFrame"
        protected val VISIBLE_FRAME = "visibleFrame"
        protected val DISPLAY_FRAME = "displayFrame"
        protected val COLOR_FRAME = "colorFrame"
        protected val DEFAULT_ACTIONS = "defaultActions"
        protected val ACTIONS = "actions"
        protected val EVENTS = "events"
        protected val INTS = "ints"
        protected val FLOATS = "floats"
        protected val STRINGS = "strings"
        protected val CANVAS = "canvas"

        protected val TRANSFORM = "transform"
        protected val PIVOT = "pivot"
        protected val AABB = "aabb"
        protected val COLOR = "color"

        protected val VERSION = "version"
        protected val COMPATIBLE_VERSION = "compatibleVersion"
        protected val FRAME_RATE = "frameRate"
        protected val TYPE = "type"
        protected val SUB_TYPE = "subType"
        protected val NAME = "name"
        protected val PARENT = "parent"
        protected val TARGET = "target"
        protected val SHARE = "share"
        protected val PATH = "path"
        protected val LENGTH = "length"
        protected val DISPLAY_INDEX = "displayIndex"
        protected val BLEND_MODE = "blendMode"
        protected val INHERIT_TRANSLATION = "inheritTranslation"
        protected val INHERIT_ROTATION = "inheritRotation"
        protected val INHERIT_SCALE = "inheritScale"
        protected val INHERIT_REFLECTION = "inheritReflection"
        protected val INHERIT_ANIMATION = "inheritAnimation"
        protected val INHERIT_FFD = "inheritFFD"
        protected val BEND_POSITIVE = "bendPositive"
        protected val CHAIN = "chain"
        protected val WEIGHT = "weight"

        protected val FADE_IN_TIME = "fadeInTime"
        protected val PLAY_TIMES = "playTimes"
        protected val SCALE = "scale"
        protected val OFFSET = "offset"
        protected val POSITION = "position"
        protected val DURATION = "duration"
        protected val TWEEN_TYPE = "tweenType"
        protected val TWEEN_EASING = "tweenEasing"
        protected val TWEEN_ROTATE = "tweenRotate"
        protected val TWEEN_SCALE = "tweenScale"
        protected val CURVE = "curve"
        protected val SOUND = "sound"
        protected val EVENT = "event"
        protected val ACTION = "action"

        protected val X = "x"
        protected val Y = "y"
        protected val SKEW_X = "skX"
        protected val SKEW_Y = "skY"
        protected val SCALE_X = "scX"
        protected val SCALE_Y = "scY"
        protected val VALUE = "value"
        protected val ROTATE = "rotate"
        protected val SKEW = "skew"

        protected val ALPHA_OFFSET = "aO"
        protected val RED_OFFSET = "rO"
        protected val GREEN_OFFSET = "gO"
        protected val BLUE_OFFSET = "bO"
        protected val ALPHA_MULTIPLIER = "aM"
        protected val RED_MULTIPLIER = "rM"
        protected val GREEN_MULTIPLIER = "gM"
        protected val BLUE_MULTIPLIER = "bM"

        protected val UVS = "uvs"
        protected val VERTICES = "vertices"
        protected val TRIANGLES = "triangles"
        protected val WEIGHTS = "weights"
        protected val SLOT_POSE = "slotPose"
        protected val BONE_POSE = "bonePose"

        protected val GOTO_AND_PLAY = "gotoAndPlay"

        protected val DEFAULT_NAME = "default"

        protected fun _getArmatureType(value: String): ArmatureType {
            when (value.toLowerCase()) {
                "stage" -> return ArmatureType.Stage

                "armature" -> return ArmatureType.Armature

                "movieclip" -> return ArmatureType.MovieClip

                else -> return ArmatureType.Armature
            }
        }

        protected fun _getDisplayType(value: String): DisplayType {
            when (value.toLowerCase()) {
                "image" -> return DisplayType.Image

                "mesh" -> return DisplayType.Mesh

                "armature" -> return DisplayType.Armature

                "boundingbox" -> return DisplayType.BoundingBox

                else -> return DisplayType.Image
            }
        }

        protected fun _getBoundingBoxType(value: String): BoundingBoxType {
            when (value.toLowerCase()) {
                "rectangle" -> return BoundingBoxType.Rectangle

                "ellipse" -> return BoundingBoxType.Ellipse

                "polygon" -> return BoundingBoxType.Polygon

                else -> return BoundingBoxType.Rectangle
            }
        }

        protected fun _getActionType(value: String): ActionType {
            when (value.toLowerCase()) {
                "play" -> return ActionType.Play

                "frame" -> return ActionType.Frame

                "sound" -> return ActionType.Sound

                else -> return ActionType.Play
            }
        }

        protected fun _getBlendMode(value: String): BlendMode {
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

        /**
         * @see BaseFactory.parseDragonBonesData
         */
        @Deprecated("已废弃，请参考 @see")
        fun parseDragonBonesData(rawData: Any): DragonBonesData? {
            return if (rawData is ArrayBuffer) {
                parseDragonBonesDataBinary(rawData)
            } else {
                parseDragonBonesDataObject(rawData)
            }
        }

        fun parseDragonBonesDataBinary(arrayBuffer: ArrayBuffer): DragonBonesData? {
            return BinaryDataParser.instance.parseDragonBonesData(arrayBuffer, 0f)
        }

        fun parseDragonBonesDataObject(obj: Any): DragonBonesData? {
            return ObjectDataParser.instance.parseDragonBonesData(obj, 0f)
        }

        fun parseDragonBonesDataJson(json: String): DragonBonesData? {
            return ObjectDataParser.instance.parseDragonBonesData(JSON.parse(json)!!, 0f)
        }

        /**
         * @see BaseFactory.parseTextureAtlasData
         */
        @Deprecated("已废弃，请参考 @see")
        @JvmOverloads
        fun parseTextureAtlasData(rawData: Any, scale: Float = 1f): Map<String, Any> {
            Console.warn("已废弃，请参考 @see")
            val textureAtlasData = HashMap<String, Any>()

            val subTextureList = getArray<Any>(rawData, DataParser.SUB_TEXTURE)
            var i = 0
            val len = subTextureList!!.length()
            while (i < len) {
                val subTextureObject = subTextureList.getObject(i)
                val subTextureName = getString(subTextureObject, DataParser.NAME)
                val subTextureRegion = Rectangle()
                var subTextureFrame: Rectangle? = null

                subTextureRegion.x = getFloat(subTextureObject, DataParser.X) / scale
                subTextureRegion.y = getFloat(subTextureObject, DataParser.Y) / scale
                subTextureRegion.width = getFloat(subTextureObject, DataParser.WIDTH) / scale
                subTextureRegion.height = getFloat(subTextureObject, DataParser.HEIGHT) / scale

                if (`in`(subTextureObject, DataParser.FRAME_WIDTH)) {
                    subTextureFrame = Rectangle()
                    subTextureFrame.x = getFloat(subTextureObject, DataParser.FRAME_X) / scale
                    subTextureFrame.y = getFloat(subTextureObject, DataParser.FRAME_Y) / scale
                    subTextureFrame.width = getFloat(subTextureObject, DataParser.FRAME_WIDTH) / scale
                    subTextureFrame.height = getFloat(subTextureObject, DataParser.FRAME_HEIGHT) / scale
                }

                val finalSubTextureFrame = subTextureFrame
                textureAtlasData[subTextureName] = object : HashMap<String, Any>() {
                    init {
                        put("region", subTextureRegion)
                        put("frame", finalSubTextureFrame)
                        put("rotated", false)
                    }
                }
                i++
            }

            return textureAtlasData
        }
    }
}

