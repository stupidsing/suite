package suite.algo;

import java.util.Random;

import suite.math.Matrix;
import suite.math.Sigmoid;
import suite.util.Copy;

public class LongShortTermMemory {

	private static Matrix mtx = new Matrix();

	private float learningRate;
	private int inputLength;
	private int memoryLength;
	private int ll;
	private int ll1;

	public LongShortTermMemory() {
		this(1f, 8, 8);
	}

	public LongShortTermMemory(float learningRate, int inputLength, int memoryLength) {
		this.learningRate = learningRate;
		this.inputLength = inputLength;
		this.memoryLength = memoryLength;
		ll = inputLength + memoryLength;
		ll1 = ll + 1;
	}

	public Unit unit() {
		return new Unit();
	}

	public class Unit {
		private float[] memory = new float[memoryLength];
		private float[] output = new float[memoryLength];
		private float[][] wf = new float[memoryLength][ll1];
		private float[][] wi = new float[memoryLength][ll1];
		private float[][] wm = new float[memoryLength][ll1];
		private float[][] wo = new float[memoryLength][ll1];

		public Unit() {
			Random random = new Random();
			double isll = 1f / Math.sqrt(ll);
			for (int i = 0; i < memoryLength; i++)
				for (int j = 0; j < ll; j++) { // random weights, bias 0

					// forget previous lifes
					wf[i][j] = 3f;

					// Xavier initialization
					wi[i][j] = (float) (random.nextGaussian() * isll);
					wm[i][j] = (float) (random.nextGaussian() * isll);
					wo[i][j] = (float) (random.nextGaussian() * isll);
				}
		}

		public float[] activateForward(float[] input) {
			return activate_(input, null);
		}

		public void propagateBackward(float[] input, float[] expected) {
			activate_(input, expected);
		}

		private float[] activate_(float[] input, float[] expected) {
			float[] memory0 = memory;
			float[] output0 = output;
			float[] io0 = new float[ll1];

			Copy.primitiveArray(input, 0, io0, 0, inputLength);
			Copy.primitiveArray(output0, 0, io0, inputLength, memoryLength);
			io0[inputLength + memoryLength] = 1f;

			float[] fs = sigmoidOn(copy(mtx.mul(wf, io0)));
			float[] is = sigmoidOn(copy(mtx.mul(wi, io0)));
			float[] ms = tanhOn(copy(mtx.mul(wm, io0)));
			float[] os = sigmoidOn(copy(mtx.mul(wo, io0)));
			float[] memory1 = mtx.of(memory = mtx.addOn(forget(memory0, fs), forget(ms, is)));
			float[] tanh_memory1 = tanhOn(memory1);
			float[] output1 = output = forget(os, tanh_memory1);

			if (expected != null) {
				float[] e_output1 = mtx.sub(expected, output1);
				float[] e_tanh_memory1 = forgetOn(os, e_output1);
				float[] e_memory1 = mtx.of(memoryLength, i -> tanhGradient(tanh_memory1[i]) * e_tanh_memory1[i]);
				float[] e_os = forgetOn(e_output1, tanh_memory1);
				float[] e_ms = forgetOn(is, e_memory1);
				float[] e_is = forgetOn(ms, e_memory1);
				float[] e_fs = forgetOn(memory0, e_memory1);
				float[] e_wo = forgetOn(e_os, sigmoidGradientOn(os));
				float[] e_wm = forgetOn(e_ms, tanhGradientOn(ms));
				float[] e_wi = forgetOn(e_is, sigmoidGradientOn(is));
				float[] e_wf = forgetOn(e_fs, sigmoidGradientOn(fs));

				for (int i = 0; i < memoryLength; i++)
					for (int j = 0; j < ll1; j++) {
						float d = learningRate * io0[j];
						wo[i][j] += d * e_wo[i];
						wm[i][j] += d * e_wm[i];
						wi[i][j] += d * e_wi[i];
						wf[i][j] += d * e_wf[i];
					}
			}

			return output1;
		}
	}

	private float[] forget(float[] fs, float[] n) {
		return forgetOn(copy(fs), n);
	}

	private float[] forgetOn(float[] m, float[] n) {
		int length = m.length;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				m[i] *= n[i];
		else
			throw new RuntimeException("Wrong matrix sizes");
		return m;
	}

	private float[] sigmoidOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = Sigmoid.sigmoid(fs[i]);
		return fs;
	}

	private float[] sigmoidGradientOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = Sigmoid.sigmoidGradient(fs[i]);
		return fs;
	}

	private float[] tanhOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = (float) Math.tanh(fs[i]);
		return fs;
	}

	private float[] tanhGradientOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = tanhGradient(fs[i]);
		return fs;
	}

	private float tanhGradient(float f) {
		return 1f - f * f;
	}

	private float[] copy(float[] m) {
		return mtx.of(m);
	}

}
