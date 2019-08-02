package suite.algo;

import static java.lang.Math.sqrt;

import java.util.Random;

import primal.primitive.FltVerbs.CopyFlt;
import suite.math.Forget;
import suite.math.Sigmoid;
import suite.math.Tanh;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;

public class LongShortTermMemory {

	private Matrix mtx = new Matrix();
	private Random random = new Random();
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
			var isll = 1d / sqrt(ll);

			for (var i = 0; i < memoryLength; i++) {
				for (var j = 0; j < ll; j++) { // random weights, bias 0

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
			return "" //
					+ "wf = " + mtx.toString(wf) //
					+ "wi = " + mtx.toString(wi) //
					+ "wm = " + mtx.toString(wm) //
					+ "wo = " + mtx.toString(wo) //
					+ "memory = " + mtx.toString(memory) + "\n" //
					+ "output = " + mtx.toString(output) + "\n";
		}

		private float[] activate_(float[] input, float[] expected) {
			var memory0 = memory;
			var output0 = output;
			var iv = new float[ll1];

			CopyFlt.array(input, 0, iv, 0, inputLength);
			CopyFlt.array(output0, 0, iv, inputLength, memoryLength);
			iv[ll] = 1f;

			var sig_fs = Sigmoid.sigmoidOn(mtx.mul(wf, iv));
			var sig_is = Sigmoid.sigmoidOn(mtx.mul(wi, iv));
			var tanh_ms = Tanh.tanhOn(mtx.mul(wm, iv));
			var sig_os = Sigmoid.sigmoidOn(mtx.mul(wo, iv));
			var memory1 = vec.copyOf(memory = vec.addOn(Forget.forget(memory0, sig_fs), Forget.forget(tanh_ms, sig_is)));
			var tanh_memory1 = Tanh.tanhOn(memory1);
			var output1 = output = Forget.forget(sig_os, tanh_memory1);

			if (expected != null) {
				var e_output1 = vec.sub(expected, output1);
				var e_tanh_memory1 = Forget.forgetOn(sig_os, e_output1);
				var e_memory1 = Forget.forgetOn(e_tanh_memory1, Tanh.tanhGradientOn(vec.copyOf(tanh_memory1)));
				var e_sig_os = Forget.forget(e_output1, tanh_memory1);
				var e_tanh_ms = Forget.forget(e_memory1, sig_is);
				var e_sig_is = Forget.forget(e_memory1, tanh_ms);
				var e_sig_fs = Forget.forget(e_memory1, memory0);
				var e_wo = Forget.forgetOn(e_sig_os, Sigmoid.sigmoidGradientOn(sig_os));
				var e_wm = Forget.forgetOn(e_tanh_ms, Tanh.tanhGradientOn(tanh_ms));
				var e_wi = Forget.forgetOn(e_sig_is, Sigmoid.sigmoidGradientOn(sig_is));
				var e_wf = Forget.forgetOn(e_sig_fs, Sigmoid.sigmoidGradientOn(sig_fs));

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

}
