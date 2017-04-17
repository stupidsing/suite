package suite.algo;

import java.util.Random;

import suite.math.Matrix;
import suite.math.Sigmoid;
import suite.math.Tanh;
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
			float[] iv = new float[ll1];

			Copy.primitiveArray(input, 0, iv, 0, inputLength);
			Copy.primitiveArray(output0, 0, iv, inputLength, memoryLength);
			iv[ll] = 1f;

			float[] sig_fs = sigmoidOn(mtx.mul(wf, iv));
			float[] sig_is = sigmoidOn(mtx.mul(wi, iv));
			float[] tanh_ms = tanhOn(mtx.mul(wm, iv));
			float[] sig_os = sigmoidOn(mtx.mul(wo, iv));
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
						float d = learningRate * iv[j];
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
			fs[i] = Tanh.tanh(fs[i]);
		return fs;
	}

	private float[] tanhGradientOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++)
			fs[i] = Tanh.tanhGradient(fs[i]);
		return fs;
	}

	private float[] copy(float[] m) {
		return mtx.of(m);
	}

}
