package suite.nn;

import java.util.Random;

import suite.math.Sigmoid;
import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;
import suite.util.To;

public class NeuralNetwork {

	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();
	private Random random = new Random();

	private float learningRate = 1f;

	public interface Layer {
		public float[] forward(float[] inputs);

		public float[] backProp(float[] inputs, float[] outputs, float[] errors);
	}

	public float[] forward(Layer[] layers, float[] ins, float[] expect) {
		int nLayers = layers.length;
		float[][] inputs = new float[nLayers][];
		float[][] outputs = new float[nLayers][];
		float[] result = ins;

		for (int i = 0; i < nLayers; i++)
			result = outputs[i] = layers[i].forward(inputs[i] = result);

		if (expect != null) {
			float[] errors = vec.sub(expect, result);

			for (int i = nLayers - 1; 0 <= i; i--)
				errors = layers[i].backProp(inputs[i], outputs[i], errors);
		}

		return result;
	}

	public class FeedForwardNnLayer implements Layer {
		private int nInputs;
		private int nOutputs;
		private float[][] weights;

		public FeedForwardNnLayer(int nInputs, int nOutputs) {
			this.nInputs = nInputs;
			this.nOutputs = nOutputs;
			weights = To.arrayOfFloats(nInputs, nOutputs, (i, j) -> random.nextFloat());
		}

		public float[] forward(float[] inputs) {
			float[] m = mtx.mul(inputs, weights);
			for (int j = 0; j < nOutputs; j++)
				m[j] = (float) activationFunction(m[j]);
			return m;
		}

		public float[] backProp(float[] inputs, float[] outputs, float[] errors) {
			for (int j = 0; j < nOutputs; j++) {
				float e = errors[j] *= (float) activationFunctionGradient(outputs[j]);
				for (int i = 0; i < nInputs; i++)
					weights[i][j] += learningRate * inputs[i] * e;
			}
			return mtx.mul(weights, errors);
		}
	}

	private double activationFunction(double value) {
		return Sigmoid.sigmoid(value);
	}

	private double activationFunctionGradient(double value) {
		return Sigmoid.sigmoidGradient(value);
	}

}
