package suite.nn;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static suite.util.Streamlet_.forInt;

import java.lang.reflect.Array;
import java.util.Random;

import primal.Verbs.New;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import suite.math.Sigmoid;
import suite.math.Tanh;
import suite.math.linalg.Matrix;
import suite.primitive.DblMutable;
import suite.primitive.Dbl_Dbl;
import suite.primitive.Floats_;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Int_Dbl;
import suite.streamlet.Puller;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.To;

public class NeuralNetwork {

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	private double initRate = 1d;
	private double learningRate = 1d;

	public interface Layer<I, O> {
		public Out<I, O> feed(I input);

		public default <U> Layer<I, U> append(Layer<O, U> layer) {
			var layer0 = this;
			var layer1 = layer;
			return inputs -> {
				var out0 = layer0.feed(inputs);
				var out1 = layer1.feed(out0.output);
				return new Out<>(out1.output, errors -> out0.backprop.apply(out1.backprop.apply(errors)));
			};
		}
	}

	public static class Out<I, O> {
		public final O output;
		public final Fun<O, I> backprop; // input errors, return errors

		public Out(O output, Fun<O, I> backprop) {
			this.output = output;
			this.backprop = backprop;
		}
	}

	public Layer<float[], float[]> ml(int[] sizes) {
		var layer = nil1dLayer();
		for (var i = 1; i < sizes.length; i++)
			layer = layer.append(feedForwardLayer(sizes[i - 1], sizes[i]));
		return layer;
	}

	public Layer<float[], float[]> mlRmsprop(int[] sizes) {
		var layer = nil1dLayer();
		for (var i = 1; i < sizes.length; i++)
			layer = layer.append(feedForwardRmspropLayer(sizes[i - 1], sizes[i]));
		return layer;
	}

	public Layer<float[][], float[][]> mlMinibatchRmsprop(int[] sizes) {
		var layer = nil2dLayer();
		for (var i = 1; i < sizes.length; i++)
			layer = layer.append(feedForwardMinibatchRmspropLayer(sizes[i - 1], sizes[i]));
		return layer;
	}

	public Layer<float[][], float[]> conv() {
		var nKernels = 9;
		var inputSize = 19;
		var kernelSize = 5;
		var maxPoolSize = 3;
		var flattenSize = (inputSize - kernelSize + 1) / maxPoolSize;
		var outputSize = 1;

		// input 19x19
		return nil2dLayer() //
				.append(spawn2dLayer(nKernels, i -> nil2dLayer() //
						.append(conv2dLayer(kernelSize, kernelSize)) //
						.append(Boolean.TRUE ? maxPoolLayer(maxPoolSize, maxPoolSize) : averagePoolLayer(maxPoolSize, maxPoolSize)) //
						.append(flattenLayer(flattenSize)) //
						.append(Boolean.TRUE ? reluLayer() : softmaxLayer()))) //
				.append(flattenLayer(flattenSize)) //
				.append(feedForwardLayer(nKernels * flattenSize, outputSize));
	}

	// https://blog.janestreet.com/deep-learning-experiments-in-ocaml/
	public Layer<float[][][], float[]> convVgg16() {
		var b = new Object() {
			private Layer<float[][][], float[][][]> nil3dLayer() {
				return nilLayer();
			}

			private Layer<float[][][], float[][][]> convVgg16Block( //
					Layer<float[][][], float[][][]> layer, //
					int n, //
					int nInputChannels, //
					int nOutputChannels) {
				var nChannels = nInputChannels;
				for (var i = 0; i < n; i++) {
					var nChannels0 = nChannels;
					layer = layer.append(convChannelLayer(nChannels0, nChannels = nOutputChannels, 3, 3));
				}
				return layer.append(channelLayer(nOutputChannels, float[][].class, c -> maxPoolLayer(2, 2)));
			}
		};

		var imageSize = 64;

		// input 64x64x3
		var layer0 = b.nil3dLayer();
		var layer1 = b.convVgg16Block(layer0, 2, 3, 64);
		var layer2 = b.convVgg16Block(layer1, 2, 64, 128);
		var layer3 = b.convVgg16Block(layer2, 4, 128, 256);
		var layer4 = b.convVgg16Block(layer3, 4, 256, 512);
		var layer5 = b.convVgg16Block(layer4, 4, 512, 512);

		return layer5 //
				.append(flattenLayer(float[][].class, imageSize - 16)) //
				.append(flattenLayer(float[].class, imageSize - 16)) //
				.append(feedForwardLayer((imageSize - 16) * (imageSize - 16) * 512, 4096)) //
				.append(feedForwardLayer(4096, 4096)) //
				.append(feedForwardLayer(4096, 1000)) //
				.append(softmaxLayer());
	}

	private Layer<float[], float[]> nil1dLayer() {
		return nilLayer();
	}

	private Layer<float[][], float[][]> nil2dLayer() {
		return nilLayer();
	}

	private <T> Layer<T, T> nilLayer() {
		return inputs -> new Out<>(inputs, errors -> errors);
	}

	private Layer<float[], float[]> feedForwardLayer(int nInputs, int nOutputs) {
		var weights = To.matrix(nInputs, nOutputs, (i, j) -> random());

		return inputs -> {
			var outputs = Sigmoid.sigmoidOn(mtx.mul(inputs, weights));

			return new Out<>(outputs, errors -> {
				var derivatives = errors;

				for (var j = 0; j < nOutputs; j++)
					derivatives[j] *= (float) Sigmoid.sigmoidGradient(outputs[j]);

				for (var i = 0; i < nInputs; i++)
					for (var j = 0; j < nOutputs; j++)
						weights[i][j] += learningRate * inputs[i] * derivatives[j];

				return mtx.mul(weights, derivatives);
			});
		};
	}

	private Layer<float[], float[]> feedForwardRmspropLayer(int nInputs, int nOutputs) {
		var learningRate_ = learningRate * .01d;
		var weights = To.matrix(nInputs, nOutputs, (i, j) -> random());
		var rmsProps = To.matrix(nInputs, nOutputs, (i, j) -> Math.abs(random()));

		return inputs -> {
			var outputs = Tanh.tanhOn(mtx.mul(inputs, weights));

			return new Out<>(outputs, errors -> {
				var derivatives = errors;

				for (var j = 0; j < nOutputs; j++)
					derivatives[j] *= (float) Tanh.tanhGradient(outputs[j]);

				for (var i = 0; i < nInputs; i++)
					for (var j = 0; j < nOutputs; j++) {
						var delta = inputs[i] * derivatives[j];
						var rmsProp = rmsProps[i][j] = (float) (rmsProps[i][j] * .99d + delta * delta * .01d);
						weights[i][j] += learningRate_ * delta / sqrt(rmsProp);
					}

				return mtx.mul(weights, derivatives);
			});
		};
	}

	// inputs :: nPoints * nInputs
	// outputs :: nPoints * nOutputs
	private Layer<float[][], float[][]> feedForwardMinibatchRmspropLayer(int nInputs, int nOutputs) {
		var learningRate_ = learningRate * .01d;
		var weights = To.matrix(nInputs, nOutputs, (i, j) -> random());
		var rmsProps = To.matrix(nInputs, nOutputs, (i, j) -> Math.abs(random()));

		return inputs -> {
			var nPoints = mtx.height(inputs);
			var outputs = mtx.mapOn(mtx.mul(inputs, weights), Tanh::tanh);

			return new Out<>(outputs, errors -> {
				var derivatives = errors;

				for (var i = 0; i < nPoints; i++)
					for (var j = 0; j < nOutputs; j++)
						derivatives[i][j] *= (float) Tanh.tanhGradient(outputs[i][j]);

				var deltas = To.matrix(nInputs, nOutputs, (i, o) -> forInt(nPoints) //
						.toDouble(Int_Dbl.sum(p -> inputs[p][i] * derivatives[p][o])));

				var deltaSqs = mtx.map(deltas, delta -> delta * delta);
				mtx.addOn(mtx.scaleOn(rmsProps, .99d), mtx.scaleOn(deltaSqs, .01d));

				var adjusts = To.matrix(nInputs, nOutputs, (i, j) -> deltas[i][j] * learningRate_ / sqrt(rmsProps[i][j]));
				return mtx.mul_mnT(derivatives, mtx.addOn(weights, adjusts)); // nPoints * nInputs
			});
		};
	}

	private Layer<float[][], float[][]> spawn2dLayer(int n, Int_Obj<Layer<float[][], float[]>> fun) {
		return spawnLayer(float[].class, n, fun, errors -> mtx.sum(errors.toArray(float[][].class)));
	}

	private Layer<float[][][], float[][][]> convChannelLayer(int nInputChannels, int nOutputChannels, int sx, int sy) {
		var cls = forInt(nOutputChannels) //
				.map(oc -> forInt(nInputChannels) //
						.map(ic -> conv2dLayer(sx, sy)) //
						.toList()) //
				.toList();

		return inputs -> {
			var input0 = inputs[0];
			var ix = mtx.height(input0);
			var iy = mtx.width(input0);
			var hsx = ix - sx + 1;
			var hsy = iy - sy + 1;

			var outs = forInt(nOutputChannels) //
					.map(oc -> forInt(nInputChannels) //
							.map(ic -> cls.get(oc).get(ic).feed(inputs[ic])) //
							.toList()) //
					.toList();

			var outputs = To.array(nOutputChannels, float[][].class, oc -> {
				var output = new float[hsx][hsy];
				for (var ic = 0; ic < nInputChannels; ic++)
					mtx.addOn(output, outs.get(oc).get(ic).output);
				return output;
			});

			return new Out<>(outputs, errors0 -> {
				return To.array(nInputChannels, float[][].class, ic -> {
					var error = new float[ix][iy];
					for (var oc = 0; oc < nOutputChannels; oc++)
						mtx.addOn(error, outs.get(oc).get(ic).backprop.apply(errors0[oc]));
					return error;
				});
			});
		};
	}

	private Layer<float[][], float[][]> conv2dLayer(int sx, int sy) {
		var kernel = To.matrix(sx, sy, (x, y) -> random());
		var bias = DblMutable.of(0d);

		return inputs -> {
			var hsx = mtx.height(inputs) - sx + 1;
			var hsy = mtx.width(inputs) - sy + 1;

			var outputs = To.matrix(hsx, hsy, (ox, oy) -> {
				var sum = bias.value();
				for (var x = 0; x < sx; x++)
					for (var y = 0; y < sy; y++)
						sum += inputs[ox + x][oy + y] * (double) kernel[x][y];
				return sum;
			});

			return new Out<>(outputs, errors -> {
				var errors1 = new float[hsx][hsy];

				for (var ox = 0; ox < hsx; ox++)
					for (var oy = 0; oy < hsy; oy++) {
						var e = errors[ox][oy] *= outputs[ox][oy];
						bias.update(bias.value() + e);
						for (var x = 0; x < sx; x++)
							for (var y = 0; y < sy; y++) {
								var ix = ox + x;
								var iy = oy + y;
								errors1[ix][iy] += e * (double) (kernel[x][y] += learningRate * inputs[ix][iy] * e);
							}
					}

				return errors1;
			});
		};
	}

	private Layer<float[][], float[][]> averagePoolLayer(int ux, int uy) {
		var maskx = ux - 1;
		var masky = uy - 1;
		var shiftx = Integer.numberOfTrailingZeros(ux);
		var shifty = Integer.numberOfTrailingZeros(uy);

		return inputs -> {
			var sx = mtx.height(inputs);
			var sy = mtx.width(inputs);
			var outputs = new float[sx + maskx >> shiftx][sy + masky >> shifty];

			for (var ix = 0; ix < sx; ix++) {
				var in = inputs[ix];
				var out = outputs[ix >> shiftx];
				for (var iy = 0; iy < sy; iy++)
					out[iy >> shifty] += in[iy];
			}

			return new Out<>(outputs, errors -> To.matrix(sx, sy, (ix, iy) -> errors[ix >> shiftx][iy >> shifty]));
		};
	}

	private Layer<float[][], float[][]> maxPoolLayer(int ux, int uy) {
		var maskx = ux - 1;
		var masky = uy - 1;
		var shiftx = Integer.numberOfTrailingZeros(ux);
		var shifty = Integer.numberOfTrailingZeros(uy);

		return inputs -> {
			var sx = mtx.height(inputs);
			var sy = mtx.width(inputs);
			var outputs = To.matrix(sx + maskx >> shiftx, sy + masky >> shifty, (x, y) -> Float.MIN_VALUE);

			for (var ix = 0; ix < sx; ix++)
				for (var iy = 0; iy < sy; iy++) {
					var ox = ix >> shiftx;
					var oy = iy >> shifty;
					outputs[ox][oy] = max(outputs[ox][oy], inputs[ix][iy]);
				}

			return new Out<>(outputs, errors -> {
				for (var ix = 0; ix < sx; ix++)
					for (var iy = 0; iy < sy; iy++) {
						var ox = ix >> shiftx;
						var oy = iy >> shifty;
						inputs[ix][iy] = inputs[ix][iy] == outputs[ox][oy] ? errors[ox][oy] : 0f;
					}
				return inputs;
			});
		};
	}

	private Layer<float[][], float[]> flattenLayer(int stride) {
		return flattenLayer(float[].class, stride);
	}

	// T must be an array type
	private <T> Layer<T[], T> flattenLayer(Class<T> arrayClazz, int stride) {
		var clazz = arrayClazz.getComponentType();

		return inputs -> {
			@SuppressWarnings("unchecked")
			var outputs = (T) New.array(clazz, inputs.length * stride);
			var di = 0;

			for (var row : inputs) {
				System.arraycopy(row, 0, outputs, di, stride);
				di += stride;
			}

			return new Out<>(outputs, errors -> {
				var errors1 = New.array(arrayClazz, Array.getLength(errors) / stride);
				var si = 0;

				for (var i = 0; i < errors1.length; i++) {
					@SuppressWarnings("unchecked")
					var t = (T) New.array(clazz, stride);
					System.arraycopy(errors, si, errors1[i] = t, 0, stride);
					si += stride;
				}

				return errors1;
			});
		};
	}

	private Layer<float[], float[]> reluLayer() {
		return inputs -> {
			var outputs = To.vector(inputs, relu::apply);
			return new Out<>(outputs, errors -> To.vector(errors, reluGradient::apply));
		};
	}

	private Dbl_Dbl relu = d -> min(0d, d);
	private Dbl_Dbl reluGradient = value -> value < 0f ? 0f : 1f;

	private Layer<float[], float[]> softmaxLayer() {
		return inputs -> {
			var exps = To.vector(inputs, Math::exp);
			var invSum = 1d / Floats_.of(exps).sum();
			var softmaxs = To.vector(exps, exp -> exp * invSum);
			return new Out<>(softmaxs, errors -> {
				var length = errors.length;
				var gradients = new float[length];
				var esms = To.vector(length, j -> errors[j] * softmaxs[j]);
				for (var i = 0; i < length; i++)
					for (var j = 0; j < length; j++) {
						var kronecker = (i == j ? 1d : 0d);
						gradients[i] += esms[j] * (kronecker - softmaxs[i]);
					}
				return gradients;
			});
		};
	}

	private <I, O> Layer<I, O[]> spawnLayer( //
			Class<O> clazz, //
			int n, //
			Int_Obj<Layer<I, O>> fun, //
			Fun<Puller<I>, I> combineErrors) {
		var layers = forInt(n).map(fun::apply);
		return spawnLayer(clazz, layers, inputs -> inputs, combineErrors);
	}

	private <I, O> Layer<I, O[]> spawnLayer( //
			Class<O> clazz, //
			Streamlet<Layer<I, O>> layers, //
			Iterate<I> cloneInputs, //
			Fun<Puller<I>, I> combineErrors) {
		return inputs -> {
			var outs = layers.map(layer -> layer.feed(cloneInputs.apply(inputs))).collect();
			var outputs = outs.map(out -> out.output).toArray(clazz);

			return new Out<>(outputs, errors -> Read //
					.from(errors) //
					.zip(outs, (error, out) -> out.backprop.apply(error)) //
					.collect(combineErrors));
		};
	}

	private <T> Layer<T[], T[]> channelLayer(int nChannels, Class<T> clazz, Int_Obj<Layer<T, T>> layerFun) {
		var layers = forInt(nChannels).map(layerFun).collect();

		return inputs -> {
			var outs = layers.zip(Read.from(inputs), Layer::feed).collect();
			var outputs = outs.map(out -> out.output).toArray(clazz);

			return new Out<>(outputs, errors -> Read //
					.from(errors) //
					.zip(outs, (error, out) -> out.backprop.apply(error)) //
					.toArray(clazz));
		};
	}

	private double random() {
		return random.nextGaussian() * initRate;
	}

}
