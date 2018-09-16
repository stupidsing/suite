package suite.nn;

import static suite.util.Friends.max;
import static suite.util.Friends.min;

import java.lang.reflect.Array;
import java.util.Random;

import suite.math.Sigmoid;
import suite.math.linalg.Matrix;
import suite.primitive.DblMutable;
import suite.primitive.Dbl_Dbl;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Ints_;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Iterate;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Array_;
import suite.util.To;

public class NeuralNetwork {

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	private float learningRate = 1f;

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
		Layer<float[], float[]> layer = nilLayer();
		for (var i = 1; i < sizes.length; i++)
			layer = layer.append(feedForwardLayer(sizes[i - 1], sizes[i]));
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
				.append(spawnLayer(nKernels, i -> nil2dLayer() //
						.append(convLayer(kernelSize, kernelSize)) //
						.append(Boolean.TRUE ? maxPoolLayer(maxPoolSize, maxPoolSize) : averagePoolLayer(maxPoolSize, maxPoolSize)) //
						.append(flattenLayer(flattenSize)) //
						.append(reluLayer()))) //
				.append(flattenLayer(flattenSize)) //
				.append(feedForwardLayer(nKernels * flattenSize, outputSize));
	}

	private Layer<float[][], float[][]> nil2dLayer() {
		return nilLayer();
	}

	private <T> Layer<T, T> nilLayer() {
		return inputs -> new Out<>(inputs, errors -> errors);
	}

	private Layer<float[], float[]> feedForwardLayer(int nInputs, int nOutputs) {
		var weights = To.matrix(nInputs, nOutputs, (i, j) -> random.nextFloat());

		return inputs -> {
			var outputs = mtx.mul(inputs, weights);

			for (var j = 0; j < nOutputs; j++)
				outputs[j] = (float) Sigmoid.sigmoid(outputs[j]);

			return new Out<>(outputs, errors -> {
				for (var j = 0; j < nOutputs; j++) {
					var e = errors[j] *= (float) Sigmoid.sigmoidGradient(outputs[j]);
					for (var i = 0; i < nInputs; i++)
						weights[i][j] += learningRate * inputs[i] * e;
				}
				return mtx.mul(weights, errors);
			});
		};
	}

	private Layer<float[][], float[][]> spawnLayer(int n, Int_Obj<Layer<float[][], float[]>> fun) {
		var layers = Ints_.range(n).map(fun::apply);

		return this.<float[][], float[]> spawnLayer(float[].class, layers, input -> input, errors0 -> {
			var errors1 = errors0.toList();
			var e = errors1.get(0);
			var sums = new float[mtx.height(e)][mtx.width(e)];
			for (var error : errors1)
				sums = mtx.add(sums, error);
			return sums;
		});
	}

	private <I, O> Layer<I, O[]> spawnLayer( //
			Class<O> clazz, //
			Streamlet<Layer<I, O>> layers, //
			Iterate<I> cloneInputs, //
			Fun<Outlet<I>, I> combineErrors) {
		var size = layers.size();

		return inputs -> {
			var outs = layers.map(layer -> layer.feed(cloneInputs.apply(inputs))).toList();
			var outputs = Read.from(outs).map(out -> out.output).toArray(clazz);

			return new Out<>(outputs, errors -> Ints_ //
					.range(size) //
					.map(i -> outs.get(i).backprop.apply(errors[i])) //
					.collect(combineErrors));
		};
	}

	private Layer<float[][], float[][]> convLayer(int sx, int sy) {
		var kernel = To.matrix(sx, sy, (x, y) -> random.nextFloat());
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

			return new Out<>(outputs, errors -> To //
					.matrix(sx, sy, (ix, iy) -> errors[ix >> shiftx][iy >> shifty]));
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
		return this.<float[]> flattenLayer(float[].class, stride);
	}

	private <T> Layer<T[], T> flattenLayer(Class<T> arrayClazz, int stride) {
		var clazz = arrayClazz.getComponentType();

		return inputs -> {
			@SuppressWarnings("unchecked")
			var outputs = (T) Array_.newArray(clazz, inputs.length * stride);
			var di = 0;

			for (var row : inputs) {
				System.arraycopy(row, 0, outputs, di, stride);
				di += stride;
			}

			return new Out<>(outputs, errors -> {
				var errors1 = Array_.newArray(arrayClazz, Array.getLength(errors) / stride);
				var si = 0;

				for (var i = 0; i < errors1.length; i++) {
					@SuppressWarnings("unchecked")
					var t = (T) Array_.newArray(clazz, stride);
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

}
