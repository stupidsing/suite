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

			for (int i = 0; i < memoryLength; i++) {
				for (int j = 0; j < ll; j++) { // random weights, bias 0

					// Xavier initialization
					wi[i][j] = (float) (random.nextGaussian() * isll);
					wm[i][j] = (float) (random.nextGaussian() * isll);
					wo[i][j] = (float) (random.nextGaussian() * isll);
				}

				// forget previous lifes
				wf[i][ll] = 3f;
			}
		}

		public float[] activateForward(float[] input) {
			return activate_(input, null);
		}

		public void propagateBackward(float[] input, float[] expected) {
			activate_(input, expected);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("wf = " + mtx.toString(wf));
			sb.append("wi = " + mtx.toString(wi));
			sb.append("wm = " + mtx.toString(wm));
			sb.append("wo = " + mtx.toString(wo));
			sb.append("memory = " + mtx.toString(memory) + "\n");
			sb.append("output = " + mtx.toString(output) + "\n");
			return sb.toString();
		}

		private float[] activate_(float[] input, float[] expected) {
			float[] memory0 = memory;
			float[] output0 = output;
			float[] io0 = new float[ll1];

			Copy.primitiveArray(input, 0, io0, 0, inputLength);
			Copy.primitiveArray(output0, 0, io0, inputLength, memoryLength);
			io0[ll] = 1f;

			float[] sig_fs = sigmoidOn(mtx.mul(wf, io0));
			float[] sig_is = sigmoidOn(mtx.mul(wi, io0));
			float[] tanh_ms = tanhOn(mtx.mul(wm, io0));
			float[] sig_os = sigmoidOn(mtx.mul(wo, io0));
			float[] memory1 = copy(memory = mtx.addOn(forget(memory0, sig_fs), forget(tanh_ms, sig_is)));
			float[] tanh_memory1 = tanhOn(memory1);
			float[] output1 = output = forget(sig_os, tanh_memory1);

			if (expected != null) {
				float[] e_output1 = mtx.sub(expected, output1);
				float[] e_tanh_memory1 = forgetOn(sig_os, e_output1);
				float[] e_memory1 = forgetOn(e_tanh_memory1, tanhGradientOn(copy(tanh_memory1)));
				float[] e_sig_os = forget(e_output1, tanh_memory1);
				float[] e_tanh_ms = forget(e_memory1, sig_is);
				float[] e_sig_is = forget(e_memory1, tanh_ms);
				float[] e_sig_fs = forget(e_memory1, memory0);
				float[] e_wo = forgetOn(e_sig_os, sigmoidGradientOn(sig_os));
				float[] e_wm = forgetOn(e_tanh_ms, tanhGradientOn(tanh_ms));
				float[] e_wi = forgetOn(e_sig_is, sigmoidGradientOn(sig_is));
				float[] e_wf = forgetOn(e_sig_fs, sigmoidGradientOn(sig_fs));

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
			fs[i] = sigmoid(fs[i]);
		return fs;
	}

	private float[] sigmoidGradientOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = sigmoidGradient(fs[i]);
		return fs;
	}

	private float[] tanhOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = (float) tanh(fs[i]);
		return fs;
	}

	private float[] tanhGradientOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = tanhGradient(fs[i]);
		return fs;
	}

	private float sigmoid(float f) {
		return Sigmoid.sigmoid(f);
	}

	private float sigmoidGradient(float f) {
		return Sigmoid.sigmoidGradient(f);
	}

	private float tanh(float f) {
		return (float) Math.tanh(f);
	}

	private float tanhGradient(float f) {
		return 1f - f * f;
	}

	private float[] copy(float[] m) {
		return mtx.of(m);
	}

}
