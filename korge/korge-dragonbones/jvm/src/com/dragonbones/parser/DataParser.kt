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

/**
 * @private
 */
abstract class DataParser {
	protected static readonly DATA_VERSION_2_3: String = "2.3";
	protected static readonly DATA_VERSION_3_0: String = "3.0";
	protected static readonly DATA_VERSION_4_0: String = "4.0";
	protected static readonly DATA_VERSION_4_5: String = "4.5";
	protected static readonly DATA_VERSION_5_0: String = "5.0";
	protected static readonly DATA_VERSION_5_5: String = "5.5";
	protected static readonly DATA_VERSION_5_6: String = "5.6";
	protected static readonly DATA_VERSION: String = DataParser.DATA_VERSION_5_6;

	protected static readonly DATA_VERSIONS: Array<string> = [
		DataParser.DATA_VERSION_4_0,
		DataParser.DATA_VERSION_4_5,
		DataParser.DATA_VERSION_5_0,
		DataParser.DATA_VERSION_5_5,
		DataParser.DATA_VERSION_5_6
	];

	protected static readonly TEXTURE_ATLAS: String = "textureAtlas";
	protected static readonly SUB_TEXTURE: String = "SubTexture";
	protected static readonly FORMAT: String = "format";
	protected static readonly IMAGE_PATH: String = "imagePath";
	protected static readonly WIDTH: String = "width";
	protected static readonly HEIGHT: String = "height";
	protected static readonly ROTATED: String = "rotated";
	protected static readonly FRAME_X: String = "frameX";
	protected static readonly FRAME_Y: String = "frameY";
	protected static readonly FRAME_WIDTH: String = "frameWidth";
	protected static readonly FRAME_HEIGHT: String = "frameHeight";

	protected static readonly DRADON_BONES: String = "dragonBones";
	protected static readonly USER_DATA: String = "userData";
	protected static readonly ARMATURE: String = "armature";
	protected static readonly CANVAS: String = "canvas";
	protected static readonly BONE: String = "bone";
	protected static readonly SURFACE: String = "surface";
	protected static readonly SLOT: String = "slot";
	protected static readonly CONSTRAINT: String = "constraint";
	protected static readonly SKIN: String = "skin";
	protected static readonly DISPLAY: String = "display";
	protected static readonly FRAME: String = "frame";
	protected static readonly IK: String = "ik";
	protected static readonly PATH_CONSTRAINT: String = "path";

	protected static readonly ANIMATION: String = "animation";
	protected static readonly TIMELINE: String = "timeline";
	protected static readonly FFD: String = "ffd";
	protected static readonly TRANSLATE_FRAME: String = "translateFrame";
	protected static readonly ROTATE_FRAME: String = "rotateFrame";
	protected static readonly SCALE_FRAME: String = "scaleFrame";
	protected static readonly DISPLAY_FRAME: String = "displayFrame";
	protected static readonly COLOR_FRAME: String = "colorFrame";
	protected static readonly DEFAULT_ACTIONS: String = "defaultActions";
	protected static readonly ACTIONS: String = "actions";
	protected static readonly EVENTS: String = "events";

	protected static readonly INTS: String = "ints";
	protected static readonly FLOATS: String = "floats";
	protected static readonly STRINGS: String = "strings";

	protected static readonly TRANSFORM: String = "transform";
	protected static readonly PIVOT: String = "pivot";
	protected static readonly AABB: String = "aabb";
	protected static readonly COLOR: String = "color";

	protected static readonly VERSION: String = "version";
	protected static readonly COMPATIBLE_VERSION: String = "compatibleVersion";
	protected static readonly FRAME_RATE: String = "frameRate";
	protected static readonly TYPE: String = "type";
	protected static readonly SUB_TYPE: String = "subType";
	protected static readonly NAME: String = "name";
	protected static readonly PARENT: String = "parent";
	protected static readonly TARGET: String = "target";
	protected static readonly STAGE: String = "stage";
	protected static readonly SHARE: String = "share";
	protected static readonly PATH: String = "path";
	protected static readonly LENGTH: String = "length";
	protected static readonly DISPLAY_INDEX: String = "displayIndex";
	protected static readonly Z_ORDER: String = "zOrder";
	protected static readonly Z_INDEX: String = "zIndex";
	protected static readonly BLEND_MODE: String = "blendMode";
	protected static readonly INHERIT_TRANSLATION: String = "inheritTranslation";
	protected static readonly INHERIT_ROTATION: String = "inheritRotation";
	protected static readonly INHERIT_SCALE: String = "inheritScale";
	protected static readonly INHERIT_REFLECTION: String = "inheritReflection";
	protected static readonly INHERIT_ANIMATION: String = "inheritAnimation";
	protected static readonly INHERIT_DEFORM: String = "inheritDeform";
	protected static readonly SEGMENT_X: String = "segmentX";
	protected static readonly SEGMENT_Y: String = "segmentY";
	protected static readonly BEND_POSITIVE: String = "bendPositive";
	protected static readonly CHAIN: String = "chain";
	protected static readonly WEIGHT: String = "weight";

	protected static readonly BLEND_TYPE: String = "blendType";
	protected static readonly FADE_IN_TIME: String = "fadeInTime";
	protected static readonly PLAY_TIMES: String = "playTimes";
	protected static readonly SCALE: String = "scale";
	protected static readonly OFFSET: String = "offset";
	protected static readonly POSITION: String = "position";
	protected static readonly DURATION: String = "duration";
	protected static readonly TWEEN_EASING: String = "tweenEasing";
	protected static readonly TWEEN_ROTATE: String = "tweenRotate";
	protected static readonly TWEEN_SCALE: String = "tweenScale";
	protected static readonly CLOCK_WISE: String = "clockwise";
	protected static readonly CURVE: String = "curve";
	protected static readonly SOUND: String = "sound";
	protected static readonly EVENT: String = "event";
	protected static readonly ACTION: String = "action";

	protected static readonly X: String = "x";
	protected static readonly Y: String = "y";
	protected static readonly SKEW_X: String = "skX";
	protected static readonly SKEW_Y: String = "skY";
	protected static readonly SCALE_X: String = "scX";
	protected static readonly SCALE_Y: String = "scY";
	protected static readonly VALUE: String = "value";
	protected static readonly ROTATE: String = "rotate";
	protected static readonly SKEW: String = "skew";
	protected static readonly ALPHA: String = "alpha";

	protected static readonly ALPHA_OFFSET: String = "aO";
	protected static readonly RED_OFFSET: String = "rO";
	protected static readonly GREEN_OFFSET: String = "gO";
	protected static readonly BLUE_OFFSET: String = "bO";
	protected static readonly ALPHA_MULTIPLIER: String = "aM";
	protected static readonly RED_MULTIPLIER: String = "rM";
	protected static readonly GREEN_MULTIPLIER: String = "gM";
	protected static readonly BLUE_MULTIPLIER: String = "bM";

	protected static readonly UVS: String = "uvs";
	protected static readonly VERTICES: String = "vertices";
	protected static readonly TRIANGLES: String = "triangles";
	protected static readonly WEIGHTS: String = "weights";
	protected static readonly SLOT_POSE: String = "slotPose";
	protected static readonly BONE_POSE: String = "bonePose";

	protected static readonly BONES: String = "bones";
	protected static readonly POSITION_MODE: String = "positionMode";
	protected static readonly SPACING_MODE: String = "spacingMode";
	protected static readonly ROTATE_MODE: String = "rotateMode";
	protected static readonly SPACING: String = "spacing";
	protected static readonly ROTATE_OFFSET: String = "rotateOffset";
	protected static readonly ROTATE_MIX: String = "rotateMix";
	protected static readonly TRANSLATE_MIX: String = "translateMix";

	protected static readonly TARGET_DISPLAY: String = "targetDisplay";
	protected static readonly CLOSED: String = "closed";
	protected static readonly CONSTANT_SPEED: String = "constantSpeed";
	protected static readonly VERTEX_COUNT: String = "vertexCount";
	protected static readonly LENGTHS: String = "lengths";

	protected static readonly GOTO_AND_PLAY: String = "gotoAndPlay";

	protected static readonly DEFAULT_NAME: String = "default";

	protected static _getArmatureType(value: String): ArmatureType {
		switch (value.toLowerCase()) {
			case "stage":
				return ArmatureType.Stage;

			case "armature":
				return ArmatureType.Armature;

			case "movieclip":
				return ArmatureType.MovieClip;

			default:
				return ArmatureType.Armature;
		}
	}

	protected static _getBoneType(value: String): BoneType {
		switch (value.toLowerCase()) {
			case "bone":
				return BoneType.Bone;

			case "surface":
				return BoneType.Surface;

			default:
				return BoneType.Bone;
		}
	}

	protected static _getPositionMode(value: String): PositionMode {
		switch (value.toLocaleLowerCase()) {
			case "percent":
				return PositionMode.Percent;

			case "fixed":
				return PositionMode.Fixed;

			default:
				return PositionMode.Percent;
		}
	}

	protected static _getSpacingMode(value: String): SpacingMode {
		switch (value.toLocaleLowerCase()) {
			case "length":
				return SpacingMode.Length;

			case "percent":
				return SpacingMode.Percent;

			case "fixed":
				return SpacingMode.Fixed;

			default:
				return SpacingMode.Length;
		}
	}

	protected static _getRotateMode(value: String): RotateMode {
		switch (value.toLocaleLowerCase()) {
			case "tangent":
				return RotateMode.Tangent;

			case "chain":
				return RotateMode.Chain;

			case "chainscale":
				return RotateMode.ChainScale;

			default:
				return RotateMode.Tangent;
		}
	}

	protected static _getDisplayType(value: String): DisplayType {
		switch (value.toLowerCase()) {
			case "image":
				return DisplayType.Image;

			case "mesh":
				return DisplayType.Mesh;

			case "armature":
				return DisplayType.Armature;

			case "boundingbox":
				return DisplayType.BoundingBox;

			case "path":
				return DisplayType.Path;

			default:
				return DisplayType.Image;
		}
	}

	protected static _getBoundingBoxType(value: String): BoundingBoxType {
		switch (value.toLowerCase()) {
			case "rectangle":
				return BoundingBoxType.Rectangle;

			case "ellipse":
				return BoundingBoxType.Ellipse;

			case "polygon":
				return BoundingBoxType.Polygon;

			default:
				return BoundingBoxType.Rectangle;
		}
	}

	protected static _getBlendMode(value: String): BlendMode {
		switch (value.toLowerCase()) {
			case "normal":
				return BlendMode.Normal;

			case "add":
				return BlendMode.Add;

			case "alpha":
				return BlendMode.Alpha;

			case "darken":
				return BlendMode.Darken;

			case "difference":
				return BlendMode.Difference;

			case "erase":
				return BlendMode.Erase;

			case "hardlight":
				return BlendMode.HardLight;

			case "invert":
				return BlendMode.Invert;

			case "layer":
				return BlendMode.Layer;

			case "lighten":
				return BlendMode.Lighten;

			case "multiply":
				return BlendMode.Multiply;

			case "overlay":
				return BlendMode.Overlay;

			case "screen":
				return BlendMode.Screen;

			case "subtract":
				return BlendMode.Subtract;

			default:
				return BlendMode.Normal;
		}
	}

	protected static _getAnimationBlendType(value: String): AnimationBlendType {
		switch (value.toLowerCase()) {
			case "none":
				return AnimationBlendType.None;

			case "1d":
				return AnimationBlendType.E1D;

			default:
				return AnimationBlendType.None;
		}
	}

	protected static _getActionType(value: String): ActionType {
		switch (value.toLowerCase()) {
			case "play":
				return ActionType.Play;

			case "frame":
				return ActionType.Frame;

			case "sound":
				return ActionType.Sound;

			default:
				return ActionType.Play;
		}
	}

	public abstract parseDragonBonesData(rawData: any, scale: Double): DragonBonesData | null;
	public abstract parseTextureAtlasData(rawData: any, textureAtlasData: TextureAtlasData, scale: Double): Boolean;
}
