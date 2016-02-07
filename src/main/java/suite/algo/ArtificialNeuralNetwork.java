package suite.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import suite.util.Util;

public class ArtificialNeuralNetwork {

	private float learningRate = 1f;
	private int nLayers;
	private List<float[][]> weightsByLayer = new ArrayList<>();

	public ArtificialNeuralNetwork(List<Integer> layerSizes, Random random) {
		nLayers = layerSizes.size() - 1;

		for (int layer = 0; layer < nLayers; layer++) {
			int nInputs = layerSizes.get(layer);
			int nOutputs = layerSizes.get(layer + 1);
			float weights[][] = new float[nInputs][nOutputs];

			for (int i = 0; i < nInputs; i++)
				for (int j = 0; j < nOutputs; j++)
					weights[i][j] = random.nextFloat();

			weightsByLayer.add(weights);
		}
	}

	public float[] feed(float inputs[]) {
		return Util.last(forwardActivations(inputs));
	}

	public void train(float inputs[], float expected[]) {
		backwardPropagate(forwardActivations(inputs), expected);
	}

	private List<float[]> forwardActivations(float values[]) {
		List<float[]> outputs = new ArrayList<>();
		outputs.add(values);

		for (int layer = 0; layer < nLayers; layer++) {
			float wij[][] = weightsByLayer.get(layer);
			int ni = wij.length;
			int nj = wij[0].length;

			float values1[] = new float[nj];

			for (int j = 0; j < nj; j++) {
				float sum = 0f;
				for (int i = 0; i < ni; i++)
					sum += values[i] * wij[i][j];
				values1[j] = activationFunction(sum);
			}

			outputs.add(values = values1);
		}

		return outputs;
	}

	private void backwardPropagate(List<float[]> activations, float expected[]) {
		float errors[] = null;

		for (int layer = nLayers; 0 < layer; layer--) {
			float wij[][] = weightsByLayer.get(layer - 1);
			int ni = wij.length;
			int nj = wij[0].length;

			float ins[] = activations.get(layer - 1);
			float outs[] = activations.get(layer);
			float diffs[] = new float[nj];

			if (layer < nLayers) {
				float wjk[][] = weightsByLayer.get(layer);
				int nk = wjk[0].length;

				for (int j = 0; j < nj; j++) {
					float sum = 0f;
					for (int k = 0; k < nk; k++)
						sum += errors[k] * wjk[j][k];
					diffs[j] = sum;
				}
			} else
				for (int j = 0; j < nj; j++)
					diffs[j] = expected[j] - outs[j];

			float errors1[] = new float[nj];

			for (int j = 0; j < nj; j++) {
				errors1[j] = diffs[j] * activationFunctionGradient(outs[j]);
				for (int i = 0; i < ni; i++)
					wij[i][j] += learningRate * errors1[j] * ins[i];
			}

			errors = errors1;
		}
	}

	private float activationFunction(float value) {
		return 1f / (1f + (float) Math.exp(-value));
	}

	private float activationFunctionGradient(float value) {
		return value * (1f - value);
	}

}
