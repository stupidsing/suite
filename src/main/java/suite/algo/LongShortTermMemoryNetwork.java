package suite.algo;

import suite.math.Matrix;
import suite.math.Sigmoid;
import suite.util.Copy;

public class LongShortTermMemoryNetwork {

	private int memoryLength = 8;
	private int inputLength = 8;
	private int outputLength = 8;
	private int ll = inputLength + outputLength + 1;

	public class Unit {
		private float[] memory = new float[memoryLength];
		private float[] output0 = new float[outputLength];
		private float[][] wf = new float[memoryLength][ll];
		private float[][] wi = new float[memoryLength][ll];
		private float[][] wc = new float[memoryLength][ll];
		private float[][] wo = new float[memoryLength][ll];

		public float[] activateForward(float[] input) {
			float[] io0 = new float[ll];
			Copy.primitiveArray(input, 0, io0, 0, inputLength);
			Copy.primitiveArray(output0, 0, io0, inputLength, outputLength);
			io0[inputLength + outputLength] = 1f;

			float[] fs = sigmoidOn(Matrix.mul(wf, io0));
			float[] is = sigmoidOn(Matrix.mul(wi, io0));
			float[] cs = tanhOn(Matrix.mul(wc, io0));
			float[] os = sigmoidOn(Matrix.mul(wo, io0));
			float[] memory1 = Matrix.addOn(forgetOn(memory, fs), forgetOn(cs, is));
			float[] output1 = forgetOn(os, tanh(memory1));

			memory = memory1;
			return output0 = output1;
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

}
