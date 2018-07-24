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

import com.dragonbones.core.*
import com.dragonbones.model.*

/**
 * @internal
 */
class Surface  :  Bone {
	public override fun toString(): String {
		return "[class dragonBones.Surface]";
	}

	private var _dX: Double;
	private var _dY: Double;
	private var _k: Double;
	private var _kX: Double;
	private var _kY: Double;

	public val _vertices:  DoubleArray = [];
	public val _deformVertices:  DoubleArray = [];
	/**
	 * - x1, y1, x2, y2, x3, y3, x4, y4, d1X, d1Y, d2X, d2Y
	 */
	private val _hullCache:  DoubleArray = [];
	/**
	 * - Inside [flag, a, b, c, d, tx, ty], Outside [flag, a, b, c, d, tx, ty]
	 */
	private val _matrixCahce:  DoubleArray = [];

	public var _bone: Bone?;

	protected fun _onClear(): Unit {
		super._onClear();

		this._dX = 0.0;
		this._dY = 0.0;
		this._k = 0.0;
		this._kX = 0.0;
		this._kY = 0.0;
		this._vertices.length = 0;
		this._deformVertices.length = 0;
		this._matrixCahce.length = 0;
		this._hullCache.length = 0;
		this._bone = null;
	}

	private fun _getAffineTransform(
		x: Double, y: Double, lX: Double, lY: Double,
		aX: Double, aY: Double, bX: Double, bY: Double, cX: Double, cY: Double,
		transform: Transform, matrix: Matrix, isDown: Boolean
	): Unit {
		val dabX = bX - aX;
		val dabY = bY - aY;
		val dacX = cX - aX;
		val dacY = cY - aY;

		transform.rotation = Math.atan2(dabY, dabX);
		transform.skew = Math.atan2(dacY, dacX) - Math.PI * 0.5 - transform.rotation;

		if (isDown) {
			transform.rotation += Math.PI;
		}

		transform.scaleX = Math.sqrt(dabX * dabX + dabY * dabY) / lX;
		transform.scaleY = Math.sqrt(dacX * dacX + dacY * dacY) / lY;
		transform.toMatrix(matrix);
		transform.x = matrix.tx = aX - (matrix.a * x + matrix.c * y);
		transform.y = matrix.ty = aY - (matrix.b * x + matrix.d * y);
	}

	private fun _updateVertices(): Unit {
		val data = this._armature.armatureData.parent;
		val geometry = (this._boneData as SurfaceData).geometry;
		val intArray = data.intArray;
		val floatArray = data.floatArray;
		val vertexCount = intArray[geometry.offset + BinaryOffset.GeometryVertexCount];
		val verticesOffset = intArray[geometry.offset + BinaryOffset.GeometryFloatOffset];
		val vertices = this._vertices;
		val animationVertices = this._deformVertices;

		if (this._parent !== null) {
			if (this._parent._boneData.type === BoneType.Surface) {
				for (var i = 0, l = vertexCount; i < l; ++i) {
					val iD = i * 2;
					val x = floatArray[verticesOffset + iD] + animationVertices[iD];
					val y = floatArray[verticesOffset + iD + 1] + animationVertices[iD + 1];
					val matrix = (this._parent as Surface)._getGlobalTransformMatrix(x, y);
					//
					vertices[iD] = matrix.a * x + matrix.c * y + matrix.tx;
					vertices[iD + 1] = matrix.b * x + matrix.d * y + matrix.ty;
				}
			}
			else {
				val parentMatrix = this._parent.globalTransformMatrix;
				for (var i = 0, l = vertexCount; i < l; ++i) {
					val iD = i * 2;
					val x = floatArray[verticesOffset + iD] + animationVertices[iD];
					val y = floatArray[verticesOffset + iD + 1] + animationVertices[iD + 1];
					//
					vertices[iD] = parentMatrix.a * x + parentMatrix.c * y + parentMatrix.tx;
					vertices[iD + 1] = parentMatrix.b * x + parentMatrix.d * y + parentMatrix.ty;
				}
			}
		}
		else {
			for (var i = 0, l = vertexCount; i < l; ++i) {
				val iD = i * 2;
				vertices[iD] = floatArray[verticesOffset + iD] + animationVertices[iD];
				vertices[iD + 1] = floatArray[verticesOffset + iD + 1] + animationVertices[iD + 1];
			}
		}
	}

	protected fun _updateGlobalTransformMatrix(isCache: Boolean): Unit {
		// tslint:disable-next-line:no-unused-expression
		isCache;

		val segmentXD = (this._boneData as SurfaceData).segmentX * 2;
		val lastIndex = this._vertices.length - 2;
		val lA = 200.0;
		//
		val raX = this._vertices[0];
		val raY = this._vertices[1];
		val rbX = this._vertices[segmentXD];
		val rbY = this._vertices[segmentXD + 1];
		val rcX = this._vertices[lastIndex];
		val rcY = this._vertices[lastIndex + 1];
		val rdX = this._vertices[lastIndex - segmentXD];
		val rdY = this._vertices[lastIndex - segmentXD + 1];
		//
		val dacX = raX + (rcX - raX) * 0.5;
		val dacY = raY + (rcY - raY) * 0.5;
		val dbdX = rbX + (rdX - rbX) * 0.5;
		val dbdY = rbY + (rdY - rbY) * 0.5;
		val aX = dacX + (dbdX - dacX) * 0.5;
		val aY = dacY + (dbdY - dacY) * 0.5;
		val bX = rbX + (rcX - rbX) * 0.5;
		val bY = rbY + (rcY - rbY) * 0.5;
		val cX = rdX + (rcX - rdX) * 0.5;
		val cY = rdY + (rcY - rdY) * 0.5;
		// TODO interpolation
		this._getAffineTransform(0.0, 0.0, lA, lA, aX, aY, bX, bY, cX, cY, this.global, this.globalTransformMatrix, false);
		this._globalDirty = false;
	}

	public fun _getGlobalTransformMatrix(x: Double, y: Double): Matrix {
		val lA = 200.0;
		val lB = 1000.0;
		if (x < -lB || lB < x || y < -lB || lB < y) {
			return this.globalTransformMatrix;
		}

		var isDown = false;
		val surfaceData = this._boneData as SurfaceData;
		val segmentX = surfaceData.segmentX;
		val segmentY = surfaceData.segmentY;
		val segmentXD = surfaceData.segmentX * 2;
		val dX = this._dX;
		val dY = this._dY;
		val indexX = Math.floor((x + lA) / dX); // -1 ~ segmentX - 1
		val indexY = Math.floor((y + lA) / dY); // -1 ~ segmentY - 1
		var matrixIndex = 0;
		var pX = indexX * dX - lA;
		var pY = indexY * dY - lA;
		//
		val matrices = this._matrixCahce;
		val helpMatrix = Surface._helpMatrix;

		if (x < -lA) {
			if (y < -lA || y >= lA) { // Out.
				return this.globalTransformMatrix;
			}
			// Left.
			isDown = y > this._kX * (x + lA) + pY;
			matrixIndex = ((segmentX * segmentY + segmentX + segmentY + segmentY + indexY) * 2 + (isDown ? 1 : 0)) * 7;

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1);
			}
			else {
				val vertexIndex = indexY * (segmentXD + 2);
				val ddX = this._hullCache[4];
				val ddY = this._hullCache[5];
				val sX = this._hullCache[2] - (segmentY - indexY) * ddX;
				val sY = this._hullCache[3] - (segmentY - indexY) * ddY;
				val vertices = this._vertices;

				if (isDown) {
					this._getAffineTransform(
						-lA, pY + dY, lB - lA, dY,
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						sX + ddX,
						sY + ddY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						Surface._helpTransform, helpMatrix, true);
				}
				else {
					this._getAffineTransform(
						-lB, pY, lB - lA, dY,
						sX,
						sY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						sX + ddX,
						sY + ddY,
						Surface._helpTransform, helpMatrix, false);
				}

				matrices[matrixIndex] = 1.0;
				matrices[matrixIndex + 1] = helpMatrix.a;
				matrices[matrixIndex + 2] = helpMatrix.b;
				matrices[matrixIndex + 3] = helpMatrix.c;
				matrices[matrixIndex + 4] = helpMatrix.d;
				matrices[matrixIndex + 5] = helpMatrix.tx;
				matrices[matrixIndex + 6] = helpMatrix.ty;
			}
		}
		else if (x >= lA) {
			if (y < -lA || y >= lA) { // Out.
				return this.globalTransformMatrix;
			}
			// Right.
			isDown = y > this._kX * (x - lB) + pY;
			matrixIndex = ((segmentX * segmentY + segmentX + indexY) * 2 + (isDown ? 1 : 0)) * 7;

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1);
			}
			else {
				val vertexIndex = (indexY + 1) * (segmentXD + 2) - 2;
				val ddX = this._hullCache[4];
				val ddY = this._hullCache[5];
				val sX = this._hullCache[0] + indexY * ddX;
				val sY = this._hullCache[1] + indexY * ddY;
				val vertices = this._vertices;

				if (isDown) {
					this._getAffineTransform(
						lB, pY + dY, lB - lA, dY,
						sX + ddX,
						sY + ddY,
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						sX,
						sY,
						Surface._helpTransform, helpMatrix, true);
				}
				else {
					this._getAffineTransform(
						lA, pY, lB - lA, dY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						sX,
						sY,
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						Surface._helpTransform, helpMatrix, false);
				}

				matrices[matrixIndex] = 1.0;
				matrices[matrixIndex + 1] = helpMatrix.a;
				matrices[matrixIndex + 2] = helpMatrix.b;
				matrices[matrixIndex + 3] = helpMatrix.c;
				matrices[matrixIndex + 4] = helpMatrix.d;
				matrices[matrixIndex + 5] = helpMatrix.tx;
				matrices[matrixIndex + 6] = helpMatrix.ty;
			}
		}
		else if (y < -lA) {
			if (x < -lA || x >= lA) { // Out.
				return this.globalTransformMatrix;
			}
			// Up.
			isDown = y > this._kY * (x - pX - dX) - lB;
			matrixIndex = ((segmentX * segmentY + indexX) * 2 + (isDown ? 1 : 0)) * 7;

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1);
			}
			else {
				val vertexIndex = indexX * 2;
				val ddX = this._hullCache[10];
				val ddY = this._hullCache[11];
				val sX = this._hullCache[8] + indexX * ddX;
				val sY = this._hullCache[9] + indexX * ddY;
				val vertices = this._vertices;

				if (isDown) {
					this._getAffineTransform(
						pX + dX, -lA, dX, lB - lA,
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						sX + ddX,
						sY + ddY,
						Surface._helpTransform, helpMatrix, true);
				}
				else {
					this._getAffineTransform(
						pX, -lB, dX, lB - lA,
						sX,
						sY,
						sX + ddX,
						sY + ddY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						Surface._helpTransform, helpMatrix, false);
				}

				matrices[matrixIndex] = 1.0;
				matrices[matrixIndex + 1] = helpMatrix.a;
				matrices[matrixIndex + 2] = helpMatrix.b;
				matrices[matrixIndex + 3] = helpMatrix.c;
				matrices[matrixIndex + 4] = helpMatrix.d;
				matrices[matrixIndex + 5] = helpMatrix.tx;
				matrices[matrixIndex + 6] = helpMatrix.ty;
			}
		}
		else if (y >= lA) {
			if (x < -lA || x >= lA) { //  Out.
				return this.globalTransformMatrix;
			}
			// Down
			isDown = y > this._kY * (x - pX - dX) + lA;
			matrixIndex = ((segmentX * segmentY + segmentX + segmentY + indexX) * 2 + (isDown ? 1 : 0)) * 7;

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1);
			}
			else {
				val vertexIndex = segmentY * (segmentXD + 2) + indexX * 2;
				val ddX = this._hullCache[10];
				val ddY = this._hullCache[11];
				val sX = this._hullCache[6] - (segmentX - indexX) * ddX;
				val sY = this._hullCache[7] - (segmentX - indexX) * ddY;
				val vertices = this._vertices;

				if (isDown) {
					this._getAffineTransform(
						pX + dX, lB, dX, lB - lA,
						sX + ddX,
						sY + ddY,
						sX,
						sY,
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						Surface._helpTransform, helpMatrix, true);
				}
				else {
					this._getAffineTransform(
						pX, lA, dX, lB - lA,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						sX,
						sY,
						Surface._helpTransform, helpMatrix, false);
				}

				matrices[matrixIndex] = 1.0;
				matrices[matrixIndex + 1] = helpMatrix.a;
				matrices[matrixIndex + 2] = helpMatrix.b;
				matrices[matrixIndex + 3] = helpMatrix.c;
				matrices[matrixIndex + 4] = helpMatrix.d;
				matrices[matrixIndex + 5] = helpMatrix.tx;
				matrices[matrixIndex + 6] = helpMatrix.ty;
			}
		}
		else { // Center.
			isDown = y > this._k * (x - pX - dX) + pY;
			matrixIndex = ((segmentX * indexY + indexX) * 2 + (isDown ? 1 : 0)) * 7;

			if (matrices[matrixIndex] > 0.0) {
				helpMatrix.copyFromArray(matrices, matrixIndex + 1);
			}
			else {
				val vertexIndex = indexX * 2 + indexY * (segmentXD + 2);
				val vertices = this._vertices;

				if (isDown) {
					this._getAffineTransform(
						pX + dX, pY + dY, dX, dY,
						vertices[vertexIndex + segmentXD + 4],
						vertices[vertexIndex + segmentXD + 5],
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						Surface._helpTransform, helpMatrix, true);
				}
				else {
					this._getAffineTransform(
						pX, pY, dX, dY,
						vertices[vertexIndex],
						vertices[vertexIndex + 1],
						vertices[vertexIndex + 2],
						vertices[vertexIndex + 3],
						vertices[vertexIndex + segmentXD + 2],
						vertices[vertexIndex + segmentXD + 3],
						Surface._helpTransform, helpMatrix, false);
				}

				matrices[matrixIndex] = 1.0;
				matrices[matrixIndex + 1] = helpMatrix.a;
				matrices[matrixIndex + 2] = helpMatrix.b;
				matrices[matrixIndex + 3] = helpMatrix.c;
				matrices[matrixIndex + 4] = helpMatrix.d;
				matrices[matrixIndex + 5] = helpMatrix.tx;
				matrices[matrixIndex + 6] = helpMatrix.ty;
			}
		}

		return helpMatrix;
	}
	/**
	 * @internal
	 * @private
	 */
	public fun init(surfaceData: SurfaceData, armatureValue: Armature): Unit {
		if (this._boneData !== null) {
			return;
		}

		super.init(surfaceData, armatureValue);

		val segmentX = surfaceData.segmentX;
		val segmentY = surfaceData.segmentY;
		val vertexCount = this._armature.armatureData.parent.intArray[surfaceData.geometry.offset + BinaryOffset.GeometryVertexCount];
		val lB = 1000.0;
		val lA = 200.0;
		//
		this._dX = lA * 2.0 / segmentX;
		this._dY = lA * 2.0 / segmentY;
		this._k = -this._dY / this._dX;
		this._kX = -this._dY / (lB - lA);
		this._kY = -(lB - lA) / this._dX;
		this._vertices.length = vertexCount * 2;
		this._deformVertices.length = vertexCount * 2;
		this._matrixCahce.length = (segmentX * segmentY + segmentX * 2 + segmentY * 2) * 2 * 7;
		this._hullCache.length = 10;

		for (var i = 0; i < vertexCount * 2; ++i) {
			this._deformVertices[i] = 0.0;
		}

		if (this._parent !== null) {
			if (this._parent.boneData.type === BoneType.Bone) {
				this._bone = this._parent;
			}
			else {
				this._bone = (this._parent as Surface)._bone;
			}
		}
	}
	/**
	 * @internal
	 */
	public fun update(cacheFrameIndex: Double): Unit {
		if (cacheFrameIndex >= 0 && this._cachedFrameIndices !== null) {
			val cachedFrameIndex = this._cachedFrameIndices[cacheFrameIndex];
			if (cachedFrameIndex >= 0 && this._cachedFrameIndex === cachedFrameIndex) { // Same cache.
				this._transformDirty = false;
			}
			else if (cachedFrameIndex >= 0) { // Has been Cached.
				this._transformDirty = true;
				this._cachedFrameIndex = cachedFrameIndex;
			}
			else {
				if (this._hasConstraint) { // Update constraints.
					for (constraint in this._armature._constraints) {
						if (constraint._root === this) {
							constraint.update();
						}
					}
				}

				if (
					this._transformDirty ||
					(this._parent !== null && this._parent._childrenTransformDirty)
				) { // Dirty.
					this._transformDirty = true;
					this._cachedFrameIndex = -1;
				}
				else if (this._cachedFrameIndex >= 0) { // Same cache, but not set index yet.
					this._transformDirty = false;
					this._cachedFrameIndices[cacheFrameIndex] = this._cachedFrameIndex;
				}
				else { // Dirty.
					this._transformDirty = true;
					this._cachedFrameIndex = -1;
				}
			}
		}
		else {
			if (this._hasConstraint) { // Update constraints.
				for (constraint in this._armature._constraints) {
					if (constraint._root === this) {
						constraint.update();
					}
				}
			}

			if (this._transformDirty || (this._parent !== null && this._parent._childrenTransformDirty)) { // Dirty.
				cacheFrameIndex = -1;
				this._transformDirty = true;
				this._cachedFrameIndex = -1;
			}
		}

		if (this._transformDirty) {
			this._transformDirty = false;
			this._childrenTransformDirty = true;
			//
			for (var i = 0, l = this._matrixCahce.length; i < l; i += 7) {
				this._matrixCahce[i] = -1.0;
			}
			//
			this._updateVertices();
			//
			if (this._cachedFrameIndex < 0) {
				val isCache = cacheFrameIndex >= 0;
				if (this._localDirty) {
					this._updateGlobalTransformMatrix(isCache);
				}

				if (isCache && this._cachedFrameIndices !== null) {
					this._cachedFrameIndex = this._cachedFrameIndices[cacheFrameIndex] = this._armature._armatureData.setCacheFrame(this.globalTransformMatrix, this.global);
				}
			}
			else {
				this._armature._armatureData.getCacheFrame(this.globalTransformMatrix, this.global, this._cachedFrameIndex);
			}
			// Update hull vertices.
			val lB = 1000.0;
			val lA = 200.0;
			val ddX = 2 * this.global.x;
			val ddY = 2 * this.global.y;
			//
			val helpPoint = Surface._helpPoint;
			this.globalTransformMatrix.transformPoint(lB, -lA, helpPoint);
			this._hullCache[0] = helpPoint.x;
			this._hullCache[1] = helpPoint.y;
			this._hullCache[2] = ddX - helpPoint.x;
			this._hullCache[3] = ddY - helpPoint.y;
			this.globalTransformMatrix.transformPoint(0.0, this._dY, helpPoint, true);
			this._hullCache[4] = helpPoint.x;
			this._hullCache[5] = helpPoint.y;
			//
			this.globalTransformMatrix.transformPoint(lA, lB, helpPoint);
			this._hullCache[6] = helpPoint.x;
			this._hullCache[7] = helpPoint.y;
			this._hullCache[8] = ddX - helpPoint.x;
			this._hullCache[9] = ddY - helpPoint.y;
			this.globalTransformMatrix.transformPoint(this._dX, 0.0, helpPoint, true);
			this._hullCache[10] = helpPoint.x;
			this._hullCache[11] = helpPoint.y;
		}
		else if (this._childrenTransformDirty) {
			this._childrenTransformDirty = false;
		}

		this._localDirty = true;
	}
}
