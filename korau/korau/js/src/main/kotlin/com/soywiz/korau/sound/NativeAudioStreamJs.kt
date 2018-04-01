package com.soywiz.korau.sound

import com.soywiz.kds.Queue
import com.soywiz.klogger.Logger
import com.soywiz.korio.async.eventLoop
import com.soywiz.korio.coroutine.getCoroutineContext
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Int16Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.events.Event
import kotlin.browser.document

external class AudioContext {
	fun createScriptProcessor(bufferSize: Int, numberOfInputChannels: Int, numberOfOutputChannels: Int): ScriptProcessorNode
	val destination: AudioDestinationNode
}

external interface AudioBuffer {
	val duration: Double
	val length: Double
	val numberOfChannels: Int
	val sampleRate: Int
	fun copyFromChannel(destination: Float32Array, channelNumber: Int, startInChannel: Double?): Unit
	fun copyToChannel(source: Float32Array, channelNumber: Int, startInChannel: Double?): Unit
	fun getChannelData(channel: Int): Float32Array
}

external interface AudioNode {
	val channelCount: Int
	//val channelCountMode: ChannelCountMode
	//val channelInterpretation: ChannelInterpretation
	val context: AudioContext
	val numberOfInputs: Int
	val numberOfOutputs: Int
	fun connect(destination: AudioNode, output: Int? = definedExternally, input: Int? = definedExternally): AudioNode
	//fun connect(destination: AudioParam, output: Int?): Unit
	fun disconnect(output: Int? = definedExternally): Unit
	fun disconnect(destination: AudioNode, output: Int? = definedExternally, input: Int? = definedExternally): Unit
	//fun disconnect(destination: AudioParam, output: Int?): Unit
}

external interface AudioDestinationNode : AudioNode {
	val maxChannelCount: Int
}

external class AudioProcessingEvent : Event {
	val inputBuffer: AudioBuffer
	val outputBuffer: AudioBuffer
	val playbackTime: Double
}

external interface ScriptProcessorNode : AudioNode {
	var onaudioprocess: (AudioProcessingEvent) -> Unit
}

actual class NativeAudioStream actual constructor(val freq: Int = 44100) {
	val id = lastId++
	val logger = Logger("NativeAudioStream.js.$id")

	companion object {
		var lastId = 0
		val context by lazy { AudioContext() }

		fun convertS16ToF32(channels: Int, input: Int16Array, leftVolume: Int, rightVolume: Int): Float32Array {
			val output = Float32Array(input.length * 2 / channels)
			val optimized = leftVolume == 1 && rightVolume == 1
			when (channels) {
				2 ->
					if (optimized) {
						for (n in 0 until output.length) output[n] = (input[n] / 32767.0).toFloat()
					} else {
						for (n in 0 until output.length step 2) {
							output[n + 0] = ((input[n + 0] / 32767.0) * leftVolume).toFloat()
							output[n + 1] = ((input[n + 1] / 32767.0) * rightVolume).toFloat()
						}
					}
				1 ->
					if (optimized) {
						var m = 0
						for (n in 0 until input.length) {
							val v = (input[n] / 32767.0).toFloat()
							output[m++] = v
							output[m++] = v
						}
					} else {
						var m = 0
						for (n in 0 until input.length) {
							val sample = (input[n] / 32767.0).toFloat()
							output[m++] = sample * leftVolume
							output[m++] = sample * rightVolume
						}
					}
			}
			return output
		}
	}

	var missingDataCount = 0
	var nodeRunning = false
	val node: ScriptProcessorNode by lazy {
		val node = context.createScriptProcessor(1024, 2, 2)
		node.onaudioprocess = { process(it) }
		node
	}

	var currentBuffer: PspAudioBuffer? = null
	val buffers = Queue<PspAudioBuffer>()

	private fun process(e: AudioProcessingEvent) {
		val left = e.outputBuffer.getChannelData(0)
		val right = e.outputBuffer.getChannelData(1)
		val sampleCount = left.length
		val hidden: Boolean = !!document.asDynamic().hidden
		var hasData = true

		for (n in 0 until sampleCount) {
			if (this.currentBuffer == null) {
				if (this.buffers.size == 0) {
					hasData = false
					break
				}

				//for (m in 0 until min(3, this.buffers.size)) {
				//	this.buffers[m].resolve()
				//}

				this.currentBuffer = this.buffers.dequeue()
			}

			val cb = this.currentBuffer!!
			if (cb.available >= 2) {
				left[n] = cb.read()
				right[n] = cb.read()
			} else {
				this.currentBuffer = null
				continue
			}

			if (hidden) {
				left[n] = 0f
				right[n] = 0f
			}
		}

		if (!hasData) {
			missingDataCount++
		}

		if (missingDataCount >= 500) {
			stop()
		}
	}

	private fun ensureInit() = run { node }

	fun start() {
		if (nodeRunning) return
		this.node.connect(context.destination)
		logger.error { "this.node.connect" }
		missingDataCount = 0
		nodeRunning = true
	}

	fun stop() {
		if (!nodeRunning) return
		this.node.disconnect()
		logger.error { "this.node.disconnect" }
		nodeRunning = false
	}

	fun ensureRunning() {
		ensureInit()
		if (!nodeRunning) {
			start()
		}
	}

	actual suspend fun addSamples(samples: ShortArray, offset: Int, size: Int): Unit {
		ensureRunning()

		val fsamples = Float32Array(size)
		for (n in 0 until size) fsamples[n] = (samples[offset + n].toFloat() / Short.MAX_VALUE.toFloat()).toFloat()
		buffers.enqueue(PspAudioBuffer(fsamples))

		while (buffers.size > 4) {
			getCoroutineContext().eventLoop.sleepNextFrame()
		}
	}
}

class PspAudioBuffer(val data: Float32Array, var readedCallback: (() -> Unit)? = null) {
	var offset: Int = 0

	fun resolve() {
		val rc = readedCallback
		readedCallback = null
		rc?.invoke()
	}

	val hasMore: Boolean get() = this.offset < this.length

	fun read() = this.data[this.offset++]

	val available: Int get() = this.length - this.offset
	val length: Int get() = this.data.length
}

/*


class Audio2Channel {
	private buffers: PspAudioBuffer[] = [];
	private node: ScriptProcessorNode;
	currentBuffer: PspAudioBuffer;

	static convertS16ToF32(channels: number, input: Int16Array, leftVolume: number, rightVolume: number) {
		var output = new Float32Array(input.length * 2 / channels);
		var optimized = leftVolume == 1.0 && rightVolume == 1.0;
		switch (channels) {
			case 2:
				if (optimized) {
					for (var n = 0; n < output.length; n++) output[n] = input[n] / 32767.0;
				} else {
					for (var n = 0; n < output.length; n += 2) {
						output[n + 0] = (input[n + 0] / 32767.0) * leftVolume;
						output[n + 1] = (input[n + 1] / 32767.0) * rightVolume;
					}
				}
				break;
			case 1:
				if (optimized) {
					for (var n = 0, m = 0; n < input.length; n++) {
						output[m++] = output[m++] = (input[n] / 32767.0);
					}
				} else {
					for (var n = 0, m = 0; n < input.length; n++) {
						var sample = (input[n] / 32767.0);
						output[m++] = sample * leftVolume;
						output[m++] = sample * rightVolume;
					}
				}
				break;
		}
		return output;
	}

	constructor(public id: number, public context: AudioContext) {
		if (this.context) {
			this.node = this.context.createScriptProcessor(1024, 2, 2);
			this.node.onaudioprocess = (e) => { this.process(e) };
		}
	}

	start() {
		if (this.node) this.node.connect(this.context.destination);
	}

	stop() {
		if (this.node) this.node.disconnect();
	}

	process(e: AudioProcessingEvent) {
		var left = e.outputBuffer.getChannelData(0);
		var right = e.outputBuffer.getChannelData(1);
		var sampleCount = left.length;
		var hidden = document.hidden;

		for (var n = 0; n < sampleCount; n++) {
			if (!this.currentBuffer) {
				if (this.buffers.length == 0) break;

				for (var m = 0; m < Math.min(3, this.buffers.length); m++) {
					this.buffers[m].resolve();
				}

				this.currentBuffer = this.buffers.shift();
				this.currentBuffer.resolve();
			}

			if (this.currentBuffer.available >= 2) {
				left[n] = this.currentBuffer.read();
				right[n] = this.currentBuffer.read();
			} else {
				this.currentBuffer = null;
				n--;
			}

			if (hidden) left[n] = right[n] = 0;
		}
	}

	playAsync(data: Float32Array): Promise2<any> {
		if (!this.node) return waitAsync(10).then(() => 0);

		if (this.buffers.length < 8) {
		//if (this.buffers.length < 16) {
			//(data.length / 2)
			this.buffers.push(new PspAudioBuffer(null, data));
			//return 0;
			return Promise2.resolve(0);
		} else {
			return new Promise2<number>((resolved, rejected) => {
				this.buffers.push(new PspAudioBuffer(resolved, data));
				return 0;
			});
		}
	}

	playDataAsync(channels: number, data: Int16Array, leftVolume: number, rightVolume: number): Promise2<any> {
		//console.log(channels, data);
		return this.playAsync(Audio2Channel.convertS16ToF32(channels, data, leftVolume, rightVolume));
	}
}

export class Html5Audio2 {
	private channels = new Map<number, Audio2Channel>();
	private context: AudioContext;

	constructor() {
		this.context = new AudioContext();
	}

	getChannel(id: number):Audio2Channel {
		if (!this.channels.has(id)) this.channels.set(id, new Audio2Channel(id, this.context));
		return this.channels.get(id);
	}

	startChannel(id: number) {
		return this.getChannel(id).start();
	}

	stopChannel(id: number) {
		return this.getChannel(id).stop();
	}

	playDataAsync(id:number, channels:number, data:Int16Array, leftVolume: number, rightVolume: number) {
		return this.getChannel(id).playDataAsync(channels, data, leftVolume, rightVolume);
	}
}

 */