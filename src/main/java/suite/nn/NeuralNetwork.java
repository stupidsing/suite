package suite.nn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import suite.math.Sigmoid;
import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;
import suite.primitive.Floats_;
import suite.primitive.Ints_;
import suite.streamlet.Outlet;
import suite.util.Array_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil2.BiFun;
import suite.util.FunUtil2.Fun2;
import suite.util.To;

public class NeuralNetwork {

	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();
	private Random random = new Random();

	private float learningRate = 1f;

	public interface Layer<I, O> {
		public O forward(I inputs);

		public I backprop(I inputs, O outputs, O errors); // may destroy inputs, outputs, errors
	}

	public Fun2<float[], float[], float[]> ml(int[] sizes) {
		List<Layer<float[], float[]>> layers = new ArrayList<>();

		for (int i = 0; i < sizes.length - 1; i++)
			layers.add(new FeedForwardNnLayer(sizes[i], sizes[i + 1]));

		return train(float[].class, layers, vec::sub);
	}

	private <T> BiFun<T, T> train(Class<T> clazz, List<Layer<T, T>> layers, BiFun<T, T> errorFun) {
		int nLayers = layers.size();

		return (ins, expect) -> {
			T[] inputs = Array_.newArray(clazz, nLayers);
			T[] outputs = Array_.newArray(clazz, nLayers);
			T result = ins;

			for (int i = 0; i < nLayers; i++)
				result = outputs[i] = layers.get(i).forward(inputs[i] = result);

			if (expect != null) {
				T errors = errorFun.apply(expect, result);

				for (int i = nLayers - 1; 0 <= i; i--)
					errors = layers.get(i).backprop(inputs[i], outputs[i], errors);
			}

			return result;
		};
	}

	private class FeedForwardNnLayer implements Layer<float[], float[]> {
		private int nInputs;
		private int nOutputs;
		private float[][] weights;

		private FeedForwardNnLayer(int nInputs, int nOutputs) {
			this.nInputs = nInputs;
			this.nOutputs = nOutputs;
			weights = To.arrayOfFloats(nInputs, nOutputs, (i, j) -> random.nextFloat());
		}

		public float[] forward(float[] inputs) {
			float[] m = mtx.mul(inputs, weights);
			for (int j = 0; j < nOutputs; j++)
				m[j] = (float) Sigmoid.sigmoid(m[j]);
			return m;
		}

		public float[] backprop(float[] inputs, float[] outputs, float[] errors) {
			for (int j = 0; j < nOutputs; j++) {
				float e = errors[j] *= (float) Sigmoid.sigmoidGradient(outputs[j]);
				for (int i = 0; i < nInputs; i++)
					weights[i][j] += learningRate * inputs[i] * e;
			}
			return mtx.mul(weights, errors);
		}
	}

	@SuppressWarnings("unused")
	private class SpawnLayer<I, O> implements Layer<I, O[]> {
		private Class<O> clazz;
		private Layer<I, O>[] layers;
		private Iterate<I> cloneInputs;
		private Fun<Outlet<I>, I> combineErrors;

		private SpawnLayer(Class<O> clazz, Layer<I, O>[] layers, Iterate<I> cloneInputs, Fun<Outlet<I>, I> combineErrors) {
			this.clazz = clazz;
			this.layers = layers;
			this.cloneInputs = cloneInputs;
			this.combineErrors = combineErrors;
		}

		public O[] forward(I inputs) {
			return To.array(layers.length, clazz, i -> layers[i].forward(inputs));
		}

		public I backprop(I inputs, O[] outputs, O[] errors) {
			return Ints_ //
					.range(layers.length) //
					.map(i -> layers[i].backprop(cloneInputs.apply(inputs), outputs[i], errors[i])) //
					.collect(combineErrors);
		}
	}

	@SuppressWarnings("unused")
	private class ConvNnLayer implements Layer<float[][], float[][]> {
		private int sx, sy;
		private float[][] kernel;
		private float bias;

		private ConvNnLayer(int sx, int sy) {
			this.sx = sx;
			this.sy = sy;
			kernel = To.arrayOfFloats(sx, sy, (x, y) -> random.nextFloat());
		}

		public float[][] forward(float[][] inputs) {
			int hsx = mtx.height(inputs) - sx + 1;
			int hsy = mtx.width(inputs) - sy + 1;
			return To.arrayOfFloats(hsx, hsy, (ox, oy) -> {
				double sum = bias;
				for (int x = 0; x < sx; x++)
					for (int y = 0; y < sy; y++)
						sum += inputs[ox + x][oy + y] * (double) kernel[x][y];
				return sum;
			});
		}

		public float[][] backprop(float[][] inputs, float[][] outputs, float[][] errors) {
			int hsx = mtx.height(inputs) - sx + 1;
			int hsy = mtx.width(inputs) - sy + 1;
			float errors1[][] = new float[hsx][hsy];
			for (int ox = 0; ox < hsx; ox++)
				for (int oy = 0; oy < hsy; oy++) {
					float e = errors[ox][oy] *= outputs[ox][oy];
					bias += e;
					for (int x = 0; x < sx; x++)
						for (int y = 0; y < sy; y++) {
							int ix = ox + x;
							int iy = oy + y;
							errors1[ix][iy] += e * (double) (kernel[x][y] += learningRate * inputs[ix][iy] * e);
						}
				}

			return errors1;
		}
	}

	@SuppressWarnings("unused")
	private class AveragePoolLayer extends PoolLayer {
		private AveragePoolLayer(int ux, int uy) { // powers of 2
			super(ux, uy);
		}

		public float[][] forward(float[][] inputs) {
			int sx = mtx.height(inputs);
			int sy = mtx.width(inputs);
			float[][] outputs = new float[sx + maskx >> shiftx][sy + masky >> shifty];
			for (int ix = 0; ix < sx; ix++) {
				float[] in = inputs[ix];
				float[] out = outputs[ix >> shiftx];
				for (int iy = 0; iy < sy; iy++)
					out[iy >> shifty] += in[iy];
			}
			return outputs;
		}

		public float[][] backprop(float[][] inputs, float[][] outputs, float[][] errors) {
			int sx = mtx.height(inputs);
			int sy = mtx.width(inputs);
			return To.arrayOfFloats(sx, sy, (ix, iy) -> errors[ix >> shiftx][iy >> shifty]);
		}
	}

	@SuppressWarnings("unused")
	private class MaxPoolLayer extends PoolLayer {
		private MaxPoolLayer(int ux, int uy) { // powers of 2
			super(ux, uy);
		}

		public float[][] forward(float[][] inputs) {
			int sx = mtx.height(inputs);
			int sy = mtx.width(inputs);
			float[][] outputs = To.arrayOfFloats(sx + maskx >> shiftx, sy + masky >> shifty, (x, y) -> Float.MIN_VALUE);
			for (int ix = 0; ix < sx; ix++)
				for (int iy = 0; iy < sy; iy++) {
					int ox = ix >> shiftx;
					int oy = iy >> shifty;
					outputs[ox][oy] = Math.max(outputs[ox][oy], inputs[ix][iy]);
				}
			return outputs;
		}

		public float[][] backprop(float[][] inputs, float[][] outputs, float[][] errors) {
			int sx = mtx.height(inputs);
			int sy = mtx.width(inputs);
			for (int ix = 0; ix < sx; ix++)
				for (int iy = 0; iy < sy; iy++) {
					int ox = ix >> shiftx;
					int oy = iy >> shifty;
					inputs[ix][iy] = inputs[ix][iy] == outputs[ox][oy] ? errors[ox][oy] : 0f;
				}
			return inputs;
		}
	}

	private abstract class PoolLayer implements Layer<float[][], float[][]> {
		protected int maskx, masky;
		protected int shiftx, shifty;

		private PoolLayer(int ux, int uy) { // powers of 2
			this.maskx = ux - 1;
			this.masky = uy - 1;
			this.shiftx = Integer.numberOfTrailingZeros(ux);
			this.shifty = Integer.numberOfTrailingZeros(uy);
		}
	}

	@SuppressWarnings("unused")
	private class FlattenLayer implements Layer<float[][], float[]> {
		private int stride;

		private FlattenLayer(int stride) {
			this.stride = stride;
		}

		public float[] forward(float[][] inputs) {
			float[] outputs = new float[inputs.length * stride];
			int di = 0;
			for (float[] row : inputs) {
				Floats_.copy(row, 0, outputs, di, stride);
				di += stride;
			}
			return outputs;
		}

		public float[][] backprop(float[][] inputs, float[] outputs, float[] errors) {
			float[][] errors1 = new float[errors.length / stride][stride];
			int si = 0;
			for (float[] row : errors1) {
				Floats_.copy(errors, si, row, 0, stride);
				si += stride;
			}
			return errors1;
		}
	}

	@SuppressWarnings("unused")
	private class ReluLayer implements Layer<float[], float[]> {
		public float[] forward(float[] inputs) {
			for (int i = 0; i < inputs.length; i++)
				inputs[i] = (float) relu(inputs[i]);
			return inputs;
		}

		public float[] backprop(float[] inputs, float[] outputs, float[] errors) {
			for (int i = 0; i < errors.length; i++)
				errors[i] = (float) reluGradient(errors[i]);
			return errors;
		}

	}

	private double relu(double d) {
		return Math.min(0d, d);
	}

	private double reluGradient(double value) {
		return value < 0f ? 0f : 1f;
	}

}
