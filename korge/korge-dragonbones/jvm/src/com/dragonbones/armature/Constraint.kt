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
package com.dragonbones.armature
/**
 * @internal
 */
abstract class Constraint  :  BaseObject {
	protected static val _helpMatrix: Matrix = new Matrix();
	protected static val _helpTransform: Transform = new Transform();
	protected static val _helpPoint: Point = new Point();
	/**
	 * - For timeline state.
	 * @internal
	 */
	public _constraintData: ConstraintData;
	protected _armature: Armature;
	/**
	 * - For sort bones.
	 * @internal
	 */
	public _target: Bone;
	/**
	 * - For sort bones.
	 * @internal
	 */
	public _root: Bone;
	protected _bone: Bone?;

	protected _onClear(): Unit {
		this._armature = null as any; //
		this._target = null as any; //
		this._root = null as any; //
		this._bone = null;
	}

	public abstract init(constraintData: ConstraintData, armature: Armature): Unit;
	public abstract update(): Unit;
	public abstract invalidUpdate(): Unit;

	public get name(): String {
		return this._constraintData.name;
	}
}
/**
 * @internal
 */
class IKConstraint  :  Constraint() {
	public static toString(): String {
		return "[class dragonBones.IKConstraint]";
	}

	private _scaleEnabled: Boolean; // TODO
	/**
	 * - For timeline state.
	 * @internal
	 */
	public _bendPositive: Boolean;
	/**
	 * - For timeline state.
	 * @internal
	 */
	public _weight: Double;

	protected _onClear(): Unit {
		super._onClear();

		this._scaleEnabled = false;
		this._bendPositive = false;
		this._weight = 1.0;
		this._constraintData = null as any;
	}

	private _computeA(): Unit {
		val ikGlobal = this._target.global;
		val global = this._root.global;
		val globalTransformMatrix = this._root.globalTransformMatrix;

		var radian = Math.atan2(ikGlobal.y - global.y, ikGlobal.x - global.x);
		if (global.scaleX < 0.0) {
			radian += Math.PI;
		}

		global.rotation += Transform.normalizeRadian(radian - global.rotation) * this._weight;
		global.toMatrix(globalTransformMatrix);
	}

	private _computeB(): Unit {
		val boneLength = (this._bone as Bone)._boneData.length;
		val parent = this._root as Bone;
		val ikGlobal = this._target.global;
		val parentGlobal = parent.global;
		val global = (this._bone as Bone).global;
		val globalTransformMatrix = (this._bone as Bone).globalTransformMatrix;

		val x = globalTransformMatrix.a * boneLength;
		val y = globalTransformMatrix.b * boneLength;
		val lLL = x * x + y * y;
		val lL = Math.sqrt(lLL);
		var dX = global.x - parentGlobal.x;
		var dY = global.y - parentGlobal.y;
		val lPP = dX * dX + dY * dY;
		val lP = Math.sqrt(lPP);
		val rawRadian = global.rotation;
		val rawParentRadian = parentGlobal.rotation;
		val rawRadianA = Math.atan2(dY, dX);

		dX = ikGlobal.x - parentGlobal.x;
		dY = ikGlobal.y - parentGlobal.y;
		val lTT = dX * dX + dY * dY;
		val lT = Math.sqrt(lTT);

		var radianA = 0.0;
		if (lL + lP <= lT || lT + lL <= lP || lT + lP <= lL) {
			radianA = Math.atan2(ikGlobal.y - parentGlobal.y, ikGlobal.x - parentGlobal.x);
			if (lL + lP <= lT) {
			}
			else if (lP < lL) {
				radianA += Math.PI;
			}
		}
		else {
			val h = (lPP - lLL + lTT) / (2.0 * lTT);
			val r = Math.sqrt(lPP - h * h * lTT) / lT;
			val hX = parentGlobal.x + (dX * h);
			val hY = parentGlobal.y + (dY * h);
			val rX = -dY * r;
			val rY = dX * r;

			var isPPR = false;
			val parentParent = parent.parent;
			if (parentParent !== null) {
				val parentParentMatrix = parentParent.globalTransformMatrix;
				isPPR = parentParentMatrix.a * parentParentMatrix.d - parentParentMatrix.b * parentParentMatrix.c < 0.0;
			}

			if (isPPR !== this._bendPositive) {
				global.x = hX - rX;
				global.y = hY - rY;
			}
			else {
				global.x = hX + rX;
				global.y = hY + rY;
			}

			radianA = Math.atan2(global.y - parentGlobal.y, global.x - parentGlobal.x);
		}

		val dR = Transform.normalizeRadian(radianA - rawRadianA);
		parentGlobal.rotation = rawParentRadian + dR * this._weight;
		parentGlobal.toMatrix(parent.globalTransformMatrix);
		//
		val currentRadianA = rawRadianA + dR * this._weight;
		global.x = parentGlobal.x + Math.cos(currentRadianA) * lP;
		global.y = parentGlobal.y + Math.sin(currentRadianA) * lP;
		//
		var radianB = Math.atan2(ikGlobal.y - global.y, ikGlobal.x - global.x);
		if (global.scaleX < 0.0) {
			radianB += Math.PI;
		}

		global.rotation = parentGlobal.rotation + rawRadian - rawParentRadian + Transform.normalizeRadian(radianB - dR - rawRadian) * this._weight;
		global.toMatrix(globalTransformMatrix);
	}

	public init(constraintData: ConstraintData, armature: Armature): Unit {
		if (this._constraintData !== null) {
			return;
		}

		this._constraintData = constraintData;
		this._armature = armature;
		this._target = this._armature.getBone(this._constraintData.target.name) as any;
		this._root = this._armature.getBone(this._constraintData.root.name) as any;
		this._bone = this._constraintData.bone !== null ? this._armature.getBone(this._constraintData.bone.name) : null;

		{
			val ikConstraintData = this._constraintData as IKConstraintData;
			this._scaleEnabled = ikConstraintData.scaleEnabled;
			this._bendPositive = ikConstraintData.bendPositive;
			this._weight = ikConstraintData.weight;
		}

		this._root._hasConstraint = true;
	}

	public update(): Unit {
		this._root.updateByConstraint();

		if (this._bone !== null) {
			this._bone.updateByConstraint();
			this._computeB();
		}
		else {
			this._computeA();
		}
	}

	public invalidUpdate(): Unit {
		this._root.invalidUpdate();

		if (this._bone !== null) {
			this._bone.invalidUpdate();
		}
	}
}

/**
 * @internal
 */
class PathConstraint  :  Constraint {

	public dirty: Boolean;
	public pathOffset: Double;
	public position: Double;
	public spacing: Double;
	public rotateOffset: Double;
	public rotateMix: Double;
	public translateMix: Double;

	private _pathSlot: Slot;
	private _bones: Array<Bone> = [];

	private _spaces:  DoubleArray = [];
	private _positions:  DoubleArray = [];
	private _curves:  DoubleArray = [];
	private _boneLengths:  DoubleArray = [];

	private _pathGlobalVertices:  DoubleArray = [];
	private _segments:  DoubleArray = [10];

	public static toString(): String {
		return "[class dragonBones.PathConstraint]";
	}

	protected _onClear(): Unit {
		super._onClear();

		this.dirty = false;
		this.pathOffset = 0;

		this.position = 0.0;
		this.spacing = 0.0;
		this.rotateOffset = 0.0;
		this.rotateMix = 1.0;
		this.translateMix = 1.0;

		this._pathSlot = null as any;
		this._bones.length = 0;

		this._spaces.length = 0;
		this._positions.length = 0;
		this._curves.length = 0;
		this._boneLengths.length = 0;

		this._pathGlobalVertices.length = 0;
	}

	protected _updatePathVertices(verticesData: GeometryData): Unit {
		//计算曲线的节点数据
		val armature = this._armature;
		val dragonBonesData = armature.armatureData.parent;
		val scale = armature.armatureData.scale;
		val intArray = dragonBonesData.intArray;
		val floatArray = dragonBonesData.floatArray;

		val pathOffset = verticesData.offset;
		val pathVertexCount = intArray[pathOffset + BinaryOffset.GeometryVertexCount];
		val pathVertexOffset = intArray[pathOffset + BinaryOffset.GeometryFloatOffset];

		this._pathGlobalVertices.length = pathVertexCount * 2;

		val weightData = verticesData.weight;
		//没有骨骼约束我,那节点只受自己的Bone控制
		if (weightData === null) {
			val parentBone = this._pathSlot.parent;
			parentBone.updateByConstraint();

			val matrix = parentBone.globalTransformMatrix;

			for (var i = 0, iV = pathVertexOffset; i < pathVertexCount; i += 2) {
				val vx = floatArray[iV++] * scale;
				val vy = floatArray[iV++] * scale;

				val x = matrix.a * vx + matrix.c * vy + matrix.tx;
				val y = matrix.b * vx + matrix.d * vy + matrix.ty;

				//
				this._pathGlobalVertices[i] = x;
				this._pathGlobalVertices[i + 1] = y;
			}
			return;
		}

		//有骨骼约束我,那我的节点受骨骼权重控制
		val bones = this._pathSlot._geometryBones;
		val weightBoneCount = weightData.bones.length;

		val weightOffset = weightData.offset;
		val floatOffset = intArray[weightOffset + BinaryOffset.WeigthFloatOffset];

		var iV = floatOffset;
		var iB = weightOffset + BinaryOffset.WeigthBoneIndices + weightBoneCount;

		for (var i = 0, iW = 0; i < pathVertexCount; i++) {
			val vertexBoneCount = intArray[iB++]; //

			var xG = 0.0, yG = 0.0;
			for (var ii = 0, ll = vertexBoneCount; ii < ll; ii++) {
				val boneIndex = intArray[iB++];
				val bone = bones[boneIndex];
				if (bone === null) {
					continue;
				}

				bone.updateByConstraint();
				val matrix = bone.globalTransformMatrix;
				val weight = floatArray[iV++];
				val vx = floatArray[iV++] * scale;
				val vy = floatArray[iV++] * scale;
				xG += (matrix.a * vx + matrix.c * vy + matrix.tx) * weight;
				yG += (matrix.b * vx + matrix.d * vy + matrix.ty) * weight;
			}

			this._pathGlobalVertices[iW++] = xG;
			this._pathGlobalVertices[iW++] = yG;
		}
	}

	protected _computeVertices(start: Double, count: Double, offset: Double, out:  DoubleArray): Unit {
		//TODO优化
		for (var i = offset, iW = start; i < count; i += 2) {
			out[i] = this._pathGlobalVertices[iW++];
			out[i + 1] = this._pathGlobalVertices[iW++];
		}
	}

	protected _computeBezierCurve(pathDisplayDta: PathDisplayData, spaceCount: Double, tangents: Boolean, percentPosition: Boolean, percentSpacing: Boolean): Unit {
		//计算当前的骨骼在曲线上的位置
		val armature = this._armature;
		val intArray = armature.armatureData.parent.intArray;
		val vertexCount = intArray[pathDisplayDta.geometry.offset + BinaryOffset.GeometryVertexCount];

		val positions = this._positions;
		val spaces = this._spaces;
		val isClosed = pathDisplayDta.closed;
		val curveVertices =  DoubleArray();
		var verticesLength = vertexCount * 2;
		var curveCount = verticesLength / 6;
		var preCurve = -1;
		var position = this.position;

		positions.length = spaceCount * 3 + 2;

		var pathLength = 0.0;
		//不需要匀速运动，效率高些
		if (!pathDisplayDta.constantSpeed) {
			val lenghts = pathDisplayDta.curveLengths;
			curveCount -= isClosed ? 1 : 2;
			pathLength = lenghts[curveCount];

			if (percentPosition) {
				position *= pathLength;
			}

			if (percentSpacing) {
				for (var i = 0; i < spaceCount; i++) {
					spaces[i] *= pathLength;
				}
			}

			curveVertices.length = 8;
			for (var i = 0, o = 0, curve = 0; i < spaceCount; i++ , o += 3) {
				val space = spaces[i];
				position += space;

				if (isClosed) {
					position %= pathLength;
					if (position < 0) {
						position += pathLength;
					}
					curve = 0;
				}
				else if (position < 0) {
					//TODO
					continue;
				}
				else if (position > pathLength) {
					//TODO
					continue;
				}

				var percent = 0.0;
				for (; ; curve++) {
					val len = lenghts[curve];
					if (position > len) {
						continue;
					}
					if (curve === 0) {
						percent = position / len;
					}
					else {
						val preLen = lenghts[curve - 1];
						percent = (position - preLen) / (len - preLen);
					}
					break;
				}

				if (curve !== preCurve) {
					preCurve = curve;
					if (isClosed && curve === curveCount) {
						//计算曲线
						this._computeVertices(verticesLength - 4, 4, 0, curveVertices);
						this._computeVertices(0, 4, 4, curveVertices);
					}
					else {
						this._computeVertices(curve * 6 + 2, 8, 0, curveVertices);
					}
				}

				//
				this.addCurvePosition(percent, curveVertices[0], curveVertices[1], curveVertices[2], curveVertices[3], curveVertices[4], curveVertices[5], curveVertices[6], curveVertices[7], positions, o, tangents);
			}

			return;
		}

		//匀速的
		if (isClosed) {
			verticesLength += 2;
			curveVertices.length = vertexCount;
			this._computeVertices(2, verticesLength - 4, 0, curveVertices);
			this._computeVertices(0, 2, verticesLength - 4, curveVertices);

			curveVertices[verticesLength - 2] = curveVertices[0];
			curveVertices[verticesLength - 1] = curveVertices[1];
		}
		else {
			curveCount--;
			verticesLength -= 4;
			curveVertices.length = verticesLength;
			this._computeVertices(2, verticesLength, 0, curveVertices);
		}
		//
		var curves:  DoubleArray = new  DoubleArray(curveCount);
		pathLength = 0;
		var x1 = curveVertices[0], y1 = curveVertices[1], cx1 = 0, cy1 = 0, cx2 = 0, cy2 = 0, x2 = 0, y2 = 0;
		var tmpx, tmpy, dddfx, dddfy, ddfx, ddfy, dfx, dfy;

		for (var i = 0, w = 2; i < curveCount; i++ , w += 6) {
			cx1 = curveVertices[w];
			cy1 = curveVertices[w + 1];
			cx2 = curveVertices[w + 2];
			cy2 = curveVertices[w + 3];
			x2 = curveVertices[w + 4];
			y2 = curveVertices[w + 5];
			tmpx = (x1 - cx1 * 2 + cx2) * 0.1875;
			tmpy = (y1 - cy1 * 2 + cy2) * 0.1875;
			dddfx = ((cx1 - cx2) * 3 - x1 + x2) * 0.09375;
			dddfy = ((cy1 - cy2) * 3 - y1 + y2) * 0.09375;
			ddfx = tmpx * 2 + dddfx;
			ddfy = tmpy * 2 + dddfy;
			dfx = (cx1 - x1) * 0.75 + tmpx + dddfx * 0.16666667;
			dfy = (cy1 - y1) * 0.75 + tmpy + dddfy * 0.16666667;
			pathLength += Math.sqrt(dfx * dfx + dfy * dfy);
			dfx += ddfx;
			dfy += ddfy;
			ddfx += dddfx;
			ddfy += dddfy;
			pathLength += Math.sqrt(dfx * dfx + dfy * dfy);
			dfx += ddfx;
			dfy += ddfy;
			pathLength += Math.sqrt(dfx * dfx + dfy * dfy);
			dfx += ddfx + dddfx;
			dfy += ddfy + dddfy;
			pathLength += Math.sqrt(dfx * dfx + dfy * dfy);
			curves[i] = pathLength;
			x1 = x2;
			y1 = y2;
		}

		if (percentPosition) {
			position *= pathLength;
		}
		if (percentSpacing) {
			for (var i = 0; i < spaceCount; i++) {
				spaces[i] *= pathLength;
			}
		}

		var segments = this._segments;
		var curveLength: Double = 0;
		for (var i = 0, o = 0, curve = 0, segment = 0; i < spaceCount; i++ , o += 3) {
			val space = spaces[i];
			position += space;
			var p = position;

			if (isClosed) {
				p %= pathLength;
				if (p < 0) p += pathLength;
				curve = 0;
			} else if (p < 0) {
				continue;
			} else if (p > pathLength) {
				continue;
			}

			// Determine curve containing position.
			for (; ; curve++) {
				val length = curves[curve];
				if (p > length) continue;
				if (curve === 0)
					p /= length;
				else {
					val prev = curves[curve - 1];
					p = (p - prev) / (length - prev);
				}
				break;
			}

			if (curve !== preCurve) {
				preCurve = curve;
				var ii = curve * 6;
				x1 = curveVertices[ii];
				y1 = curveVertices[ii + 1];
				cx1 = curveVertices[ii + 2];
				cy1 = curveVertices[ii + 3];
				cx2 = curveVertices[ii + 4];
				cy2 = curveVertices[ii + 5];
				x2 = curveVertices[ii + 6];
				y2 = curveVertices[ii + 7];
				tmpx = (x1 - cx1 * 2 + cx2) * 0.03;
				tmpy = (y1 - cy1 * 2 + cy2) * 0.03;
				dddfx = ((cx1 - cx2) * 3 - x1 + x2) * 0.006;
				dddfy = ((cy1 - cy2) * 3 - y1 + y2) * 0.006;
				ddfx = tmpx * 2 + dddfx;
				ddfy = tmpy * 2 + dddfy;
				dfx = (cx1 - x1) * 0.3 + tmpx + dddfx * 0.16666667;
				dfy = (cy1 - y1) * 0.3 + tmpy + dddfy * 0.16666667;
				curveLength = Math.sqrt(dfx * dfx + dfy * dfy);
				segments[0] = curveLength;
				for (ii = 1; ii < 8; ii++) {
					dfx += ddfx;
					dfy += ddfy;
					ddfx += dddfx;
					ddfy += dddfy;
					curveLength += Math.sqrt(dfx * dfx + dfy * dfy);
					segments[ii] = curveLength;
				}
				dfx += ddfx;
				dfy += ddfy;
				curveLength += Math.sqrt(dfx * dfx + dfy * dfy);
				segments[8] = curveLength;
				dfx += ddfx + dddfx;
				dfy += ddfy + dddfy;
				curveLength += Math.sqrt(dfx * dfx + dfy * dfy);
				segments[9] = curveLength;
				segment = 0;
			}

			// Weight by segment length.
			p *= curveLength;
			for (; ; segment++) {
				val length = segments[segment];
				if (p > length) continue;
				if (segment === 0)
					p /= length;
				else {
					val prev = segments[segment - 1];
					p = segment + (p - prev) / (length - prev);
				}
				break;
			}

			this.addCurvePosition(p * 0.1, x1, y1, cx1, cy1, cx2, cy2, x2, y2, positions, o, tangents);
		}
	}

	//Calculates a point on the curve, for a given t value between 0 and 1.
	private addCurvePosition(t: Double, x1: Double, y1: Double, cx1: Double, cy1: Double, cx2: Double, cy2: Double, x2: Double, y2: Double, out:  DoubleArray, offset: Double, tangents: Boolean) {
		if (t === 0) {
			out[offset] = x1;
			out[offset + 1] = y1;
			out[offset + 2] = 0;
			return;
		}

		if (t === 1) {
			out[offset] = x2;
			out[offset + 1] = y2;
			out[offset + 2] = 0;
			return;
		}

		val mt = 1 - t;
		val mt2 = mt * mt;
		val t2 = t * t;
		val a = mt2 * mt;
		val b = mt2 * t * 3;
		val c = mt * t2 * 3;
		val d = t * t2;

		val x = a * x1 + b * cx1 + c * cx2 + d * x2;
		val y = a * y1 + b * cy1 + c * cy2 + d * y2;

		out[offset] = x;
		out[offset + 1] = y;
		if (tangents) {
			//Calculates the curve tangent at the specified t value
			out[offset + 2] = Math.atan2(y - (a * y1 + b * cy1 + c * cy2), x - (a * x1 + b * cx1 + c * cx2));
		}
		else {
			out[offset + 2] = 0;
		}
	}

	public init(constraintData: ConstraintData, armature: Armature): Unit {
		this._constraintData = constraintData;
		this._armature = armature;

		var data = constraintData as PathConstraintData;

		this.pathOffset = data.pathDisplayData.geometry.offset;

		//
		this.position = data.position;
		this.spacing = data.spacing;
		this.rotateOffset = data.rotateOffset;
		this.rotateMix = data.rotateMix;
		this.translateMix = data.translateMix;

		//
		this._root = this._armature.getBone(data.root.name) as Bone;
		this._target = this._armature.getBone(data.target.name) as Bone;
		this._pathSlot = this._armature.getSlot(data.pathSlot.name) as Slot;

		for (var i = 0, l = data.bones.length; i < l; i++) {
			val bone = this._armature.getBone(data.bones[i].name);
			if (bone !== null) {
				this._bones.push(bone);
			}
		}

		if (data.rotateMode === RotateMode.ChainScale) {
			this._boneLengths.length = this._bones.length;
		}

		this._root._hasConstraint = true;
	}

	public update(): Unit {
		val pathSlot = this._pathSlot;

		if (
			pathSlot._geometryData === null ||
			pathSlot._geometryData.offset !== this.pathOffset
		) {
			return;
		}

		val constraintData = this._constraintData as PathConstraintData;

		//

		//曲线节点数据改变:父亲bone改变，权重bones改变，变形顶点改变
		var isPathVerticeDirty = false;
		if (this._root._childrenTransformDirty) {
			this._updatePathVertices(pathSlot._geometryData);
			isPathVerticeDirty = true;
		}
		else if (pathSlot._verticesDirty || pathSlot._isBonesUpdate()) {
			this._updatePathVertices(pathSlot._geometryData);
			pathSlot._verticesDirty = false;
			isPathVerticeDirty = true;
		}

		if (!isPathVerticeDirty && !this.dirty) {
			return;
		}

		//
		val positionMode = constraintData.positionMode;
		val spacingMode = constraintData.spacingMode;
		val rotateMode = constraintData.rotateMode;

		val bones = this._bones;

		val isLengthMode = spacingMode === SpacingMode.Length;
		val isChainScaleMode = rotateMode === RotateMode.ChainScale;
		val isTangentMode = rotateMode === RotateMode.Tangent;
		val boneCount = bones.length;
		val spacesCount = isTangentMode ? boneCount : boneCount + 1;

		val spacing = this.spacing;
		var spaces = this._spaces;
		spaces.length = spacesCount;

		//计曲线间隔和长度
		if (isChainScaleMode || isLengthMode) {
			//Bone改变和spacing改变触发
			spaces[0] = 0;
			for (var i = 0, l = spacesCount - 1; i < l; i++) {
				val bone = bones[i];
				bone.updateByConstraint();
				val boneLength = bone._boneData.length;
				val matrix = bone.globalTransformMatrix;
				val x = boneLength * matrix.a;
				val y = boneLength * matrix.b;

				val len = Math.sqrt(x * x + y * y);
				if (isChainScaleMode) {
					this._boneLengths[i] = len;
				}
				spaces[i + 1] = (boneLength + spacing) * len / boneLength;
			}
		}
		else {
			for (var i = 0; i < spacesCount; i++) {
				spaces[i] = spacing;
			}
		}

		//
		this._computeBezierCurve(((pathSlot._displayFrame as DisplayFrame).rawDisplayData as PathDisplayData), spacesCount, isTangentMode, positionMode === PositionMode.Percent, spacingMode === SpacingMode.Percent);

		//根据新的节点数据重新采样
		val positions = this._positions;
		var rotateOffset = this.rotateOffset;
		var boneX = positions[0], boneY = positions[1];
		var tip: Boolean;
		if (rotateOffset === 0) {
			tip = rotateMode === RotateMode.Chain;
		}
		else {
			tip = false;
			val bone = pathSlot.parent;
			if (bone !== null) {
				val matrix = bone.globalTransformMatrix;
				rotateOffset *= matrix.a * matrix.d - matrix.b * matrix.c > 0 ? Transform.DEG_RAD : - Transform.DEG_RAD;
			}
		}

		//
		val rotateMix = this.rotateMix;
		val translateMix = this.translateMix;
		for (var i = 0, p = 3; i < boneCount; i++ , p += 3) {
			var bone = bones[i];
			bone.updateByConstraint();
			var matrix = bone.globalTransformMatrix;
			matrix.tx += (boneX - matrix.tx) * translateMix;
			matrix.ty += (boneY - matrix.ty) * translateMix;

			val x = positions[p], y = positions[p + 1];
			val dx = x - boneX, dy = y - boneY;
			if (isChainScaleMode) {
				val lenght = this._boneLengths[i];

				val s = (Math.sqrt(dx * dx + dy * dy) / lenght - 1) * rotateMix + 1;
				matrix.a *= s;
				matrix.b *= s;
			}

			boneX = x;
			boneY = y;
			if (rotateMix > 0) {
				var a = matrix.a, b = matrix.b, c = matrix.c, d = matrix.d, r, cos, sin;
				if (isTangentMode) {
					r = positions[p - 1];
				}
				else {
					r = Math.atan2(dy, dx);
				}

				r -= Math.atan2(b, a);

				if (tip) {
					cos = Math.cos(r);
					sin = Math.sin(r);

					val length = bone._boneData.length;
					boneX += (length * (cos * a - sin * b) - dx) * rotateMix;
					boneY += (length * (sin * a + cos * b) - dy) * rotateMix;
				}
				else {
					r += rotateOffset;
				}

				if (r > Transform.PI) {
					r -= Transform.PI_D;
				}
				else if (r < - Transform.PI) {
					r += Transform.PI_D;
				}

				r *= rotateMix;

				cos = Math.cos(r);
				sin = Math.sin(r);

				matrix.a = cos * a - sin * b;
				matrix.b = sin * a + cos * b;
				matrix.c = cos * c - sin * d;
				matrix.d = sin * c + cos * d;
			}

			bone.global.fromMatrix(matrix);
		}

		this.dirty = false;
	}

	public invalidUpdate(): Unit {

	}
}
