package suite.algo;

import java.util.Random;

import suite.math.Forget;
import suite.math.Sigmoid;
import suite.math.Tanh;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.primitive.Floats_;

public class LongShortTermMemory {

	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

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
			var isll = 1f / Math.sqrt(ll);

			for (var i = 0; i < memoryLength; i++) {
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
			var memory0 = memory;
			var output0 = output;
			var iv = new float[ll1];

			Floats_.copy(input, 0, iv, 0, inputLength);
			Floats_.copy(output0, 0, iv, inputLength, memoryLength);
			iv[ll] = 1f;

			float[] sig_fs = Sigmoid.sigmoidOn(mtx.mul(wf, iv));
			float[] sig_is = Sigmoid.sigmoidOn(mtx.mul(wi, iv));
			float[] tanh_ms = Tanh.tanhOn(mtx.mul(wm, iv));
			float[] sig_os = Sigmoid.sigmoidOn(mtx.mul(wo, iv));
			float[] memory1 = copy(memory = vec.addOn(Forget.forget(memory0, sig_fs), Forget.forget(tanh_ms, sig_is)));
			var tanh_memory1 = Tanh.tanhOn(memory1);
			float[] output1 = output = Forget.forget(sig_os, tanh_memory1);

			if (expected != null) {
				float[] e_output1 = vec.sub(expected, output1);
				float[] e_tanh_memory1 = Forget.forgetOn(sig_os, e_output1);
				float[] e_memory1 = Forget.forgetOn(e_tanh_memory1, Tanh.tanhGradientOn(copy(tanh_memory1)));
				float[] e_sig_os = Forget.forget(e_output1, tanh_memory1);
				float[] e_tanh_ms = Forget.forget(e_memory1, sig_is);
				float[] e_sig_is = Forget.forget(e_memory1, tanh_ms);
				float[] e_sig_fs = Forget.forget(e_memory1, memory0);
				float[] e_wo = Forget.forgetOn(e_sig_os, Sigmoid.sigmoidGradientOn(sig_os));
				float[] e_wm = Forget.forgetOn(e_tanh_ms, Tanh.tanhGradientOn(tanh_ms));
				float[] e_wi = Forget.forgetOn(e_sig_is, Sigmoid.sigmoidGradientOn(sig_is));
				float[] e_wf = Forget.forgetOn(e_sig_fs, Sigmoid.sigmoidGradientOn(sig_fs));

				for (var i = 0; i < memoryLength; i++)
					for (var j = 0; j < ll1; j++) {
						var d = learningRate * iv[j];
						wo[i][j] += d * e_wo[i];
						wm[i][j] += d * e_wm[i];
						wi[i][j] += d * e_wi[i];
						wf[i][j] += d * e_wf[i];
					}
			}

			return output1;
		}
	}

	private float[] copy(float[] m) {
		return vec.of(m);
	}

}
