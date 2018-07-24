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
import com.dragonbones.geom.*

/**
 * - The texture atlas data.
 * @version DragonBones 3.0
 * @language en_US
 */
/**
 * - 贴图集数据。
 * @version DragonBones 3.0
 * @language zh_CN
 */
abstract class TextureAtlasData  :  BaseObject {
	/**
	 * @private
	 */
	public var autoSearch: Boolean;
	/**
	 * @private
	 */
	public var width: Double;
	/**
	 * @private
	 */
	public var height: Double;
	/**
	 * @private
	 */
	public var scale: Double;
	/**
	 * - The texture atlas name.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 贴图集名称。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var name: String;
	/**
	 * - The image path of the texture atlas.
	 * @version DragonBones 3.0
	 * @language en_US
	 */
	/**
	 * - 贴图集图片路径。
	 * @version DragonBones 3.0
	 * @language zh_CN
	 */
	public var imagePath: String;
	/**
	 * @private
	 */
	public val textures: Map<TextureData> = {};

	protected fun _onClear(): Unit {
		for (var k in this.textures) {
			this.textures[k].returnToPool();
			delete this.textures[k];
		}

		this.autoSearch = false;
		this.width = 0;
		this.height = 0;
		this.scale = 1.0;
		// this.textures.clear();
		this.name = "";
		this.imagePath = "";
	}
	/**
	 * @private
	 */
	public fun copyFrom(value: TextureAtlasData): Unit {
		this.autoSearch = value.autoSearch;
		this.scale = value.scale;
		this.width = value.width;
		this.height = value.height;
		this.name = value.name;
		this.imagePath = value.imagePath;

		for (k in this.textures.keys) {
			this.textures[k].returnToPool();
			delete this.textures[k];
		}

		// this.textures.clear();

		for (k in value.textures.keys) {
			val texture = this.createTexture();
			texture.copyFrom(value.textures[k]);
			this.textures[k] = texture;
		}
	}
	/**
	 * @internal
	 */
	public abstract createTexture(): TextureData;
	/**
	 * @internal
	 */
	public fun addTexture(value: TextureData): Unit {
		if (value.name in this.textures) {
			console.warn("Same texture: " + value.name);
			return;
		}

		value.parent = this;
		this.textures[value.name] = value;
	}
	/**
	 * @private
	 */
	public fun getTexture(textureName: String): TextureData? {
		return if (textureName in this.textures) this.textures[textureName] else null;
	}
}
/**
 * @private
 */
abstract class TextureData  : BaseObject() {
	companion object {
		public fun createRectangle(): Rectangle {
			return Rectangle();
		}
	}

	public var rotated: Boolean;
	public var name: String;
	public val region: Rectangle = Rectangle();
	public var parent: TextureAtlasData;
	public var frame: Rectangle? = null; // Initial value.

	protected fun _onClear(): Unit {
		this.rotated = false;
		this.name = "";
		this.region.clear();
		this.parent = null as any; //
		this.frame = null;
	}

	public fun copyFrom(value: TextureData): Unit {
		this.rotated = value.rotated;
		this.name = value.name;
		this.region.copyFrom(value.region);
		this.parent = value.parent;

		if (this.frame === null && value.frame !== null) {
			this.frame = TextureData.createRectangle();
		}
		else if (this.frame !== null && value.frame === null) {
			this.frame = null;
		}

		if (this.frame !== null && value.frame !== null) {
			this.frame.copyFrom(value.frame);
		}
	}
}
