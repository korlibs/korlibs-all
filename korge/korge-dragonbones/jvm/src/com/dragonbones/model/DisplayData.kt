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

/**
 * @private
 */
class GeometryData {
	public isShared: Boolean;
	public inheritDeform: Boolean;
	public offset: Double;
	public data: DragonBonesData;
	public weight: WeightData? = null; // Initial value.

	public clear(): Unit {
		if (!this.isShared && this.weight !== null) {
			this.weight.returnToPool();
		}

		this.isShared = false;
		this.inheritDeform = false;
		this.offset = 0;
		this.data = null as any;
		this.weight = null;
	}

	public shareFrom(value: GeometryData): Unit {
		this.isShared = true;
		this.offset = value.offset;
		this.weight = value.weight;
	}

	public get vertexCount(): Double {
		const intArray = this.data.intArray;
		return intArray[this.offset + dragonBones.BinaryOffset.GeometryVertexCount];
	}

	public get triangleCount(): Double {
		const intArray = this.data.intArray;
		return intArray[this.offset + dragonBones.BinaryOffset.GeometryTriangleCount];
	}
}
/**
 * @private
 */
abstract class DisplayData  :  BaseObject {
	public type: DisplayType;
	public name: String;
	public path: String;
	public readonly transform: Transform = new Transform();
	public parent: SkinData;

	protected _onClear(): Unit {
		this.name = "";
		this.path = "";
		this.transform.identity();
		this.parent = null as any; //
	}
}
/**
 * @private
 */
class ImageDisplayData  :  DisplayData {
	public static toString(): String {
		return "[class dragonBones.ImageDisplayData]";
	}

	public readonly pivot: Point = new Point();
	public texture: TextureData?;

	protected _onClear(): Unit {
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
	public static toString(): String {
		return "[class dragonBones.ArmatureDisplayData]";
	}

	public inheritAnimation: Boolean;
	public readonly actions: Array<ActionData> = [];
	public armature: ArmatureData?;

	protected _onClear(): Unit {
		super._onClear();

		for (const action of this.actions) {
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
	public addAction(value: ActionData): Unit {
		this.actions.push(value);
	}
}
/**
 * @private
 */
class MeshDisplayData  :  DisplayData {
	public static toString(): String {
		return "[class dragonBones.MeshDisplayData]";
	}

	public readonly geometry: GeometryData = new GeometryData();
	public texture: TextureData?;

	protected _onClear(): Unit {
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
	public static toString(): String {
		return "[class dragonBones.BoundingBoxDisplayData]";
	}

	public boundingBox: BoundingBoxData? = null; // Initial value.

	protected _onClear(): Unit {
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
	public static toString(): String {
		return "[class dragonBones.PathDisplayData]";
	}
	public closed: Boolean;
	public constantSpeed: Boolean;
	public readonly geometry: GeometryData = new GeometryData();
	public readonly curveLengths:  DoubleArray = [];

	protected _onClear(): Unit {
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
class WeightData  :  BaseObject {
	public static toString(): String {
		return "[class dragonBones.WeightData]";
	}

	public count: Double;
	public offset: Double;
	public readonly bones: Array<BoneData> = [];

	protected _onClear(): Unit {
		this.count = 0;
		this.offset = 0;
		this.bones.length = 0;
	}

	public addBone(value: BoneData): Unit {
		this.bones.push(value);
	}
}
