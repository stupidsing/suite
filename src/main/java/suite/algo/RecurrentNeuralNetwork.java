package suite.algo;

import primal.Verbs.Build;
import primal.primitive.FltVerbs.CopyFlt;
import suite.math.Forget;
import suite.math.Tanh;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;

import java.util.Random;

import static java.lang.Math.sqrt;

public class RecurrentNeuralNetwork {

	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	private float learningRate;
	private int inputLength;
	private int memoryLength;
	private int ll;
	private int ll1;

	public RecurrentNeuralNetwork() {
		this(1f, 8, 8);
	}

	public RecurrentNeuralNetwork(float learningRate, int inputLength, int memoryLength) {
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
		private float[][] weights = new float[memoryLength][ll1];

		public Unit() {
			var random = new Random();
			var isll = 1f / sqrt(ll);

			// random weights, bias 0; Xavier initialization
			for (var i = 0; i < memoryLength; i++)
				for (var j = 0; j < ll; j++)
					weights[i][j] = (float) (random.nextGaussian() * isll);
		}

		public float[] activateForward(float[] input) {
			return activate_(input, null);
		}

		public void propagateBackward(float[] input, float[] expected) {
			activate_(input, expected);
		}

		@Override
		public String toString() {
			return Build.string(sb -> {
				sb.append("weights = " + mtx.toString(weights));
				sb.append("memory = " + mtx.toString(memory) + "\n");
			});
		}

		private float[] activate_(float[] input, float[] expected) {
			var memory0 = memory;
			var iv = new float[ll1];

			CopyFlt.array(input, 0, iv, 0, inputLength);
			CopyFlt.array(memory0, 0, iv, inputLength, memoryLength);
			iv[ll] = 1f;

			var memory1 = vec.copyOf(memory = Tanh.tanhOn(mtx.mul(weights, iv)));

			if (expected != null) {
				var e_memory1 = vec.sub(expected, memory1);
				var e_weights = Forget.forgetOn(e_memory1, Tanh.tanhGradientOn(memory1));

				for (var i = 0; i < memoryLength; i++)
					for (var j = 0; j < ll1; j++)
						weights[i][j] += learningRate * e_weights[i] * iv[j];
			}

			return memory1;
		}
	}

}
