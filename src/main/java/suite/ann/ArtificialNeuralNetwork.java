package suite.ann;

import java.util.ArrayList;
import java.util.List;

import suite.util.Util;

public class ArtificialNeuralNetwork {

	private float learningRate = 0.01f;

	private int nLayers;
	private List<float[][]> weightsByLayer; // nLayers elements

	public float[] feed(float inputs[]) {
		return Util.last(calculateactivations(inputs));
	}

	public void train(float inputs[], float expected[]) {
		backwardPropagate(calculateactivations(inputs), expected);
	}

	public List<float[]> calculateactivations(float values[]) {
		List<float[]> outputs = new ArrayList<>();
		outputs.add(values);

		for (int layer = 0; layer < nLayers; layer++) {
			float weights[][] = weightsByLayer.get(layer);
			int nInputs = weights.length;
			int nOutputs = weights[0].length;

			float values1[] = new float[nOutputs];

			for (int j = 0; j < nOutputs; j++) {
				float sum = 0f;
				for (int i = 0; i < nInputs; i++)
					sum += values[i] * weights[i][j];
				values1[j] = activationFunction(sum);
			}

			outputs.add(values = values1);
		}

		return outputs;
	}

	private void backwardPropagate(List<float[]> activations, float expected[]) {
		float errors[] = null;

		for (int layer = nLayers; layer >= 0; layer--) {
			float weights[][] = weightsByLayer.get(layer - 1);
			int nInputs = weights.length;
			int nOutputs = weights[0].length;

			float ins[] = activations.get(layer - 1);
			float outs[] = activations.get(layer);

			float diffs[] = new float[nOutputs];

			if (layer < nLayers)
				for (int i = 0; i < nInputs; i++) {
					float sum = 0f;
					for (int j = 0; j < nOutputs; j++)
						sum += errors[j] * weights[i][j];
					diffs[i] = sum;
				}
			else
				for (int i = 0; i < expected.length; i++)
					diffs[i] = expected[i] - outs[i];

			float errors1[] = new float[nInputs];

			for (int i = 0; i < nInputs; i++) {
				errors1[i] = diffs[i] * activationFunctionGradient(outs[i]) * ins[i];
				for (int j = 0; j < nOutputs; j++)
					weights[i][j] += learningRate * errors1[i] * ins[i];
			}

			errors = errors1;
		}
	}

	private float activationFunction(float value) {
		return 1f / (1f + (float) Math.exp(-value));
	}

	private float activationFunctionGradient(float value) {
		return value * (1 - value);
	}

}
