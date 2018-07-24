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
package com.dragonbones.model

import com.dragonbones.core.*

/**
 * @private
 */
class GeometryData {
	public var isShared: Boolean;
	public var inheritDeform: Boolean;
	public var offset: Double;
	public var data: DragonBonesData;
	public var weight: WeightData? = null; // Initial value.

	public fun clear(): Unit {
		if (!this.isShared && this.weight !== null) {
			this.weight.returnToPool();
		}

		this.isShared = false;
		this.inheritDeform = false;
		this.offset = 0;
		this.data = null as any;
		this.weight = null;
	}

	public fun shareFrom(value: GeometryData): Unit {
		this.isShared = true;
		this.offset = value.offset;
		this.weight = value.weight;
	}

	public val vertexCount: Double get() {
		val intArray = this.data.intArray;
		return intArray[this.offset + dragonBones.BinaryOffset.GeometryVertexCount];
	}

	public val triangleCount: Double get() {
		val intArray = this.data.intArray;
		return intArray[this.offset + dragonBones.BinaryOffset.GeometryTriangleCount];
	}
}
/**
 * @private
 */
abstract class DisplayData  : BaseObject() {
	public var type: DisplayType;
	public var name: String;
	public var path: String;
	public var val transform: Transform = new Transform();
	public var parent: SkinData;

	protected fun _onClear(): Unit {
		this.name = "";
		this.path = "";
		this.transform.identity();
		this.parent = null as any; //
	}
}
/**
 * @private
 */
class ImageDisplayData  :  DisplayData() {
	public override fun toString(): String {
		return "[class dragonBones.ImageDisplayData]";
	}

	public val pivot: Point = new Point();
	public var texture: TextureData?;

	protected fun _onClear(): Unit {
		super._onClear();

		this.type = DisplayType.Image;
		this.pivot.clear();
		this.texture = null;
	}
}
/**
 * @private
 */
class ArmatureDisplayData  :  DisplayData {
	public override fun toString(): String {
		return "[class dragonBones.ArmatureDisplayData]";
	}

	public var inheritAnimation: Boolean;
	public val actions: Array<ActionData> = [];
	public var armature: ArmatureData?;

	protected fun _onClear(): Unit {
		super._onClear();

		for (action in this.actions) {
			action.returnToPool();
		}

		this.type = DisplayType.Armature;
		this.inheritAnimation = false;
		this.actions.length = 0;
		this.armature = null;
	}
	/**
	 * @private
	 */
	public fun addAction(value: ActionData): Unit {
		this.actions.push(value);
	}
}
/**
 * @private
 */
class MeshDisplayData  :  DisplayData() {
	public override fun toString(): String {
		return "[class dragonBones.MeshDisplayData]";
	}

	public val geometry: GeometryData = new GeometryData();
	public var texture: TextureData?;

	protected fun _onClear(): Unit {
		super._onClear();

		this.type = DisplayType.Mesh;
		this.geometry.clear();
		this.texture = null;
	}
}
/**
 * @private
 */
class BoundingBoxDisplayData  :  DisplayData {
	public override fun toString(): String {
		return "[class dragonBones.BoundingBoxDisplayData]";
	}

	public var boundingBox: BoundingBoxData? = null; // Initial value.

	protected fun _onClear(): Unit {
		super._onClear();

		if (this.boundingBox !== null) {
			this.boundingBox.returnToPool();
		}

		this.type = DisplayType.BoundingBox;
		this.boundingBox = null;
	}
}
/**
 * @private
 */
class PathDisplayData  :  DisplayData {
	public override fun toString(): String {
		return "[class dragonBones.PathDisplayData]";
	}
	public var closed: Boolean;
	public var constantSpeed: Boolean;
	public val geometry: GeometryData = GeometryData();
	public val curveLengths:  DoubleArray = [];

	protected fun _onClear(): Unit {
		super._onClear();

		this.type = DisplayType.Path;
		this.closed = false;
		this.constantSpeed = false;
		this.geometry.clear();
		this.curveLengths.length = 0;
	}
}
/**
 * @private
 */
class WeightData  :  BaseObject() {
	public override fun toString(): String {
		return "[class dragonBones.WeightData]";
	}

	public var count: Double;
	public var offset: Double;
	public val bones: Array<BoneData> = [];

	protected fun _onClear(): Unit {
		this.count = 0;
		this.offset = 0;
		this.bones.size = 0;
	}

	public fun addBone(value: BoneData): Unit {
		this.bones.push(value);
	}
}
