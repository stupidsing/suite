package suite.algo;

import suite.math.Matrix;
import suite.math.Sigmoid;
import suite.util.Copy;

public class LongShortTermMemory {

	private float learningRate = 1f;
	private int memoryLength = 8;
	private int inputLength = 8;
	private int outputLength = 8;
	private int ll = inputLength + outputLength + 1;

	public class Unit {
		private float[] memory = new float[memoryLength];
		private float[] output = new float[outputLength];
		private float[][] wf = new float[memoryLength][ll];
		private float[][] wi = new float[memoryLength][ll];
		private float[][] wc = new float[memoryLength][ll];
		private float[][] wo = new float[memoryLength][ll];

		public float[] activateForward(float[] input) {
			return activate_(input, null);
		}

		public void propagateBackward(float[] input, float[] expected) {
			activate_(input, expected);
		}

		public float[] activate_(float[] input, float[] expected) {
			float[] memory0 = memory;
			float[] output0 = output;

			float[] io0 = new float[ll];
			Copy.primitiveArray(input, 0, io0, 0, inputLength);
			Copy.primitiveArray(output0, 0, io0, inputLength, outputLength);
			io0[inputLength + outputLength] = 1f;

			float[] fs = sigmoidOn(Matrix.mul(wf, io0));
			float[] is = sigmoidOn(Matrix.mul(wi, io0));
			float[] cs = tanhOn(Matrix.mul(wc, io0));
			float[] os = sigmoidOn(Matrix.mul(wo, io0));
			float[] memory1 = memory = Matrix.addOn(forget(memory0, fs), forget(cs, is));
			float[] tanh_memory1 = tanh(memory1);
			float[] output1 = output = forget(os, tanh_memory1);

			if (expected != null) {
				float[] e_output1 = Matrix.sub(expected, output1);
				float[] e_tanh_memory1 = forgetOn(os, e_output1);
				float[] e_memory1 = tanhGradientOn(e_tanh_memory1);
				float[] e_os = forgetOn(tanh_memory1, e_output1);
				float[] e_cs = forget(e_memory1, is);
				float[] e_is = forget(e_memory1, cs);
				float[] e_fs = forget(e_memory1, memory0);
				float[] e_wo = sigmoidGradientOn(e_os);
				float[] e_wc = tanhGradientOn(e_cs);
				float[] e_wi = sigmoidGradientOn(e_is);
				float[] e_wf = sigmoidGradientOn(e_fs);

				for (int j = 0; j < outputLength; j++)
					for (int i = 0; i < ll; i++) {
						float d = learningRate * io0[i];
						wo[i][j] += d * e_wo[j];
						wc[i][j] += d * e_wc[j];
						wi[i][j] += d * e_wi[j];
						wf[i][j] += d * e_wf[j];
					}
			}

			return output1;
		}

		private float[] forget(float[] fs, float[] n) {
			return forgetOn(Matrix.of(fs), n);
		}

		private float[] forgetOn(float[] m, float[] n) {
			int length = m.length;
			if (length == n.length) {
				for (int i = 0; i < length; i++)
					m[i] *= n[i];
				return m;
			} else
				throw new RuntimeException("Wrong matrix sizes");
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

		private float[] tanh(float[] fs) {
			return tanhOn(Matrix.of(fs));
		}

		private float[] tanhOn(float[] fs) {
			int length = fs.length;
			for (int i = 0; i < length; i++)
				fs[i] = (float) Math.tanh(fs[i]);
			return fs;
		}
	}

	private float[] tanhGradientOn(float[] fs) {
		int length = fs.length;
		for (int i = 0; i < length; i++) {
			float f = fs[i];
			fs[i] = 1 - f * f;
		}
		return fs;
	}

}
