package suite.nn;

import java.util.List;
import java.util.Random;

import suite.math.Sigmoid;
import suite.math.linalg.Matrix_;
import suite.primitive.DblMutable;
import suite.primitive.Floats_;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Ints_;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.util.Array_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.To;

public class NeuralNetwork {

	private Matrix_ mtx = new Matrix_();
	private Random random = new Random();

	private float learningRate = 1f;

	public interface Layer<I, O> {
		public Out<I, O> feed(I input);

		public default <U> Layer<I, U> append(Layer<O, U> layer) {
			Layer<I, O> layer0 = this;
			Layer<O, U> layer1 = layer;
			return new Layer<>() {
				public Out<I, U> feed(I inputs) {
					Out<I, O> out0 = layer0.feed(inputs);
					Out<O, U> out1 = layer1.feed(out0.output);
					return new Out<>(out1.output, errors -> out0.backprop.apply(out1.backprop.apply(errors)));
				}
			};
		}
	}

	public static class Out<I, O> {
		public O output;
		public Fun<O, I> backprop; // input errors, return errors

		public Out(O output, Fun<O, I> backprop) {
			this.output = output;
			this.backprop = backprop;
		}
	}

	public Layer<float[], float[]> ml(int[] sizes) {
		Layer<float[], float[]> layer = new NilLayer<float[]>();
		for (int i = 1; i < sizes.length; i++)
			layer = compose(layer, feedForward(sizes[i - 1], sizes[i]));
		return layer;
	}

	public Layer<float[][], float[]> conv() {
		int nKernels = 9;
		int inputSize = 19;
		int kernelSize = 5;
		int maxPoolSize = 3;
		int flattenSize = (inputSize - kernelSize + 1) / maxPoolSize;
		int outputSize = 1;

		return new NilLayer<float[][]>() //
				.append(spawnLayer(nKernels, i -> new NilLayer<float[][]>() // input 19x19
						.append(convLayer(kernelSize, kernelSize)) //
						.append(Boolean.TRUE ? maxPoolLayer(maxPoolSize, maxPoolSize) : averagePoolLayer(maxPoolSize, maxPoolSize)) //
						.append(flattenLayer(flattenSize)) //
						.append(reluLayer()))) //
				.append(flattenLayer(flattenSize)) //
				.append(feedForward(nKernels * flattenSize, outputSize));
	}

	private <I, J, K> Layer<I, K> compose(Layer<I, J> layer0, Layer<J, K> layer1) {
		return new Layer<>() {
			public Out<I, K> feed(I inputs) {
				Out<I, J> out0 = layer0.feed(inputs);
				Out<J, K> out1 = layer1.feed(out0.output);
				return new Out<>(out1.output, errors -> out0.backprop.apply(out1.backprop.apply(errors)));
			}
		};
	}

	private class NilLayer<I> implements Layer<I, I> {
		public Out<I, I> feed(I inputs) {
			return new Out<>(inputs, errors -> errors);
		}
	}

	private Layer<float[], float[]> feedForward(int nInputs, int nOutputs) {
		float[][] weights = To.arrayOfFloats(nInputs, nOutputs, (i, j) -> random.nextFloat());

		return inputs -> {
			float[] outputs = mtx.mul(inputs, weights);
			for (int j = 0; j < nOutputs; j++)
				outputs[j] = (float) Sigmoid.sigmoid(outputs[j]);

			return new Out<>(outputs, errors -> {
				for (int j = 0; j < nOutputs; j++) {
					float e = errors[j] *= (float) Sigmoid.sigmoidGradient(outputs[j]);
					for (int i = 0; i < nInputs; i++)
						weights[i][j] += learningRate * inputs[i] * e;
				}
				return mtx.mul(weights, errors);
			});
		};
	}

	private Layer<float[][], float[][]> spawnLayer(int n, Int_Obj<Layer<float[][], float[]>> fun) {
		List<Layer<float[][], float[]>> layers = Ints_.range(n).map(fun::apply).toList();
		return this.<float[][], float[]> spawnLayer(float[].class, layers, input -> input, errors -> new float[0][]);
	}

	private <I, O> Layer<I, O[]> spawnLayer( //
			Class<O> clazz, //
			List<Layer<I, O>> layers, //
			Iterate<I> cloneInputs, //
			Fun<Outlet<I>, I> combineErrors) {
		int size = layers.size();

		return inputs -> {
			@SuppressWarnings("unchecked")
			Out<I, O>[] outs = To.array(size, Out.class, i -> layers.get(i).feed(cloneInputs.apply(inputs)));
			O[] outputs = Read.from(outs).map(out -> out.output).toArray(clazz);

			return new Out<>(outputs, errors -> Ints_ //
					.range(size) //
					.map(i -> outs[i].backprop.apply(errors[i])) //
					.collect(combineErrors));
		};
	}

	private Layer<float[][], float[][]> convLayer(int sx, int sy) {
		float[][] kernel = To.arrayOfFloats(sx, sy, (x, y) -> random.nextFloat());
		DblMutable bias = DblMutable.of(0d);

		return inputs -> {
			int hsx = mtx.height(inputs) - sx + 1;
			int hsy = mtx.width(inputs) - sy + 1;
			float[][] outputs = To.arrayOfFloats(hsx, hsy, (ox, oy) -> {
				double sum = bias.get();
				for (int x = 0; x < sx; x++)
					for (int y = 0; y < sy; y++)
						sum += inputs[ox + x][oy + y] * (double) kernel[x][y];
				return sum;
			});

			return new Out<>(outputs, errors -> {
				float errors1[][] = new float[hsx][hsy];
				for (int ox = 0; ox < hsx; ox++)
					for (int oy = 0; oy < hsy; oy++) {
						float e = errors[ox][oy] *= outputs[ox][oy];
						bias.update(bias.get() + e);
						for (int x = 0; x < sx; x++)
							for (int y = 0; y < sy; y++) {
								int ix = ox + x;
								int iy = oy + y;
								errors1[ix][iy] += e * (double) (kernel[x][y] += learningRate * inputs[ix][iy] * e);
							}
					}

				return errors1;
			});
		};
	}

	private Layer<float[][], float[][]> averagePoolLayer(int ux, int uy) {
		int maskx = ux - 1;
		int masky = uy - 1;
		int shiftx = Integer.numberOfTrailingZeros(ux);
		int shifty = Integer.numberOfTrailingZeros(uy);

		return inputs -> {
			int sx = mtx.height(inputs);
			int sy = mtx.width(inputs);
			float[][] outputs = new float[sx + maskx >> shiftx][sy + masky >> shifty];
			for (int ix = 0; ix < sx; ix++) {
				float[] in = inputs[ix];
				float[] out = outputs[ix >> shiftx];
				for (int iy = 0; iy < sy; iy++)
					out[iy >> shifty] += in[iy];
			}

			return new Out<>(outputs, errors -> To //
					.arrayOfFloats(sx, sy, (ix, iy) -> errors[ix >> shiftx][iy >> shifty]));
		};
	}

	private Layer<float[][], float[][]> maxPoolLayer(int ux, int uy) {
		int maskx = ux - 1;
		int masky = uy - 1;
		int shiftx = Integer.numberOfTrailingZeros(ux);
		int shifty = Integer.numberOfTrailingZeros(uy);

		return inputs -> {
			int sx = mtx.height(inputs);
			int sy = mtx.width(inputs);
			float[][] outputs = To.arrayOfFloats(sx + maskx >> shiftx, sy + masky >> shifty, (x, y) -> Float.MIN_VALUE);

			for (int ix = 0; ix < sx; ix++)
				for (int iy = 0; iy < sy; iy++) {
					int ox = ix >> shiftx;
					int oy = iy >> shifty;
					outputs[ox][oy] = Math.max(outputs[ox][oy], inputs[ix][iy]);
				}

			return new Out<>(outputs, errors -> {
				for (int ix = 0; ix < sx; ix++)
					for (int iy = 0; iy < sy; iy++) {
						int ox = ix >> shiftx;
						int oy = iy >> shifty;
						inputs[ix][iy] = inputs[ix][iy] == outputs[ox][oy] ? errors[ox][oy] : 0f;
					}
				return inputs;
			});
		};
	}

	@SuppressWarnings("unused")
	private <T> Layer<T[][], T[]> flattenLayer(Class<T[]> arrayClazz, int stride) {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) arrayClazz.getComponentType();
		return inputs -> {
			T[] outputs = Array_.newArray(clazz, inputs.length * stride);
			int di = 0;
			for (T[] row : inputs) {
				Array_.copy(row, 0, outputs, di, stride);
				di += stride;
			}

			return new Out<>(outputs, errors -> {
				T[][] errors1 = Array_.newArray(arrayClazz, errors.length / stride);
				int si = 0;
				for (int i = 0; i < errors1.length; i++) {
					Array_.copy(errors, si, errors1[i] = Array_.newArray(clazz, stride), 0, stride);
					si += stride;
				}
				return errors1;
			});
		};
	}

	private Layer<float[][], float[]> flattenLayer(int stride) {
		return inputs -> {
			float[] outputs = new float[inputs.length * stride];
			int di = 0;
			for (float[] row : inputs) {
				Floats_.copy(row, 0, outputs, di, stride);
				di += stride;
			}

			return new Out<>(outputs, errors -> {
				float[][] errors1 = new float[errors.length / stride][stride];
				int si = 0;
				for (float[] row : errors1) {
					Floats_.copy(errors, si, row, 0, stride);
					si += stride;
				}
				return errors1;
			});
		};
	}

	private Layer<float[], float[]> reluLayer() {
		return inputs -> {
			float[] outputs = To.arrayOfFloats(inputs, NeuralNetwork.this::relu);
			return new Out<>(outputs, errors -> To.arrayOfFloats(errors, NeuralNetwork.this::reluGradient));
		};
	}

	private double relu(double d) {
		return Math.min(0d, d);
	}

	private double reluGradient(double value) {
		return value < 0f ? 0f : 1f;
	}

}
