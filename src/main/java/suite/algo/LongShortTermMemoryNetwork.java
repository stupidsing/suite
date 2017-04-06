package suite.algo;

import suite.math.Matrix;
import suite.math.Sigmoid;
import suite.util.Copy;

public class LongShortTermMemoryNetwork {

	private int memoryLength = 8;
	private int inputLength = 8;
	private int outputLength = 8;
	private int ll = inputLength + outputLength;

	public class Unit {
		private float memory[] = new float[memoryLength];
		private float output0[] = new float[outputLength];
		private float wf[][] = new float[memoryLength][ll];
		private float wi[][] = new float[memoryLength][ll];
		private float wc[][] = new float[memoryLength][ll];
		private float wo[][] = new float[memoryLength][ll];
		private float bf[] = new float[memoryLength];
		private float bi[] = new float[memoryLength];
		private float bc[] = new float[memoryLength];
		private float bo[] = new float[memoryLength];

		public float[] activateForward(float input[]) {
			float io0[] = new float[ll];
			Copy.primitiveArray(input, 0, io0, 0, inputLength);
			Copy.primitiveArray(output0, 0, io0, inputLength, outputLength);

			float fs[] = sigmoid(Matrix.add(Matrix.mul(wf, io0), bf));
			float is[] = sigmoid(Matrix.add(Matrix.mul(wi, io0), bi));
			float cs[] = tanh(Matrix.add(Matrix.mul(wc, io0), bc));
			float os[] = sigmoid(Matrix.add(Matrix.mul(wo, io0), bo));
			float memory1[] = Matrix.add(forget(memory, fs), forget(cs, is));
			float output1[] = forget(os, tanh(memory1));

			memory = memory1;
			return output0 = output1;
		}

		private float[] forget(float m[], float n[]) {
			int length = m.length;
			if (length == n.length) {
				float fs1[] = new float[length];
				for (int i = 0; i < length; i++)
					fs1[i] = m[i] * n[i];
				return fs1;
			} else
				throw new RuntimeException("Wrong matrix sizes");
		}

		private float[] sigmoid(float fs0[]) {
			int length = fs0.length;
			float fs1[] = new float[length];
			for (int i = 0; i < length; i++)
				fs1[i] = Sigmoid.sigmoid(fs0[i]);
			return fs1;
		}

		private float[] tanh(float fs0[]) {
			int length = fs0.length;
			float fs1[] = new float[length];
			for (int i = 0; i < length; i++)
				fs1[i] = (float) Math.tanh(fs0[i]);
			return fs1;
		}
	}

}
