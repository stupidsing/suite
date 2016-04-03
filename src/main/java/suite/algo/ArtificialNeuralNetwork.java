package suite.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import suite.util.Util;

public class ArtificialNeuralNetwork {

	private float learningRate = 1f;
	private int nLayers;
	private List<float[][]> weightsByLayer = new ArrayList<>();
	private List<LayerWeight> lws = new ArrayList<>();

	private class LayerWeight {
		private int nInputs;
		private int nOutputs;
		private float weights[][];

		private LayerWeight(int nInputs, int nOutputs, float weights[][]) {
			this.nInputs = nInputs;
			this.nOutputs = nOutputs;
			this.weights = weights;
		}
	}

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

		for (int layer = 0; layer < nLayers; layer++) {
			int nInputs = layerSizes.get(layer);
			int nOutputs = layerSizes.get(layer + 1);
			float weights[][] = new float[nInputs][nOutputs];

			for (int i = 0; i < nInputs; i++)
				for (int j = 0; j < nOutputs; j++)
					weights[i][j] = random.nextFloat();

			lws.add(new LayerWeight(nInputs, nOutputs, weights));
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

		for (LayerWeight lw : lws) {
			float values1[] = new float[lw.nOutputs];

			for (int j = 0; j < lw.nOutputs; j++) {
				float sum = 0f;
				for (int i = 0; i < lw.nInputs; i++)
					sum += values[i] * lw.weights[i][j];
				values1[j] = activationFunction(sum);
			}

			outputs.add(values = values1);
		}

		return outputs;
	}

	private void backwardPropagate(List<float[]> activations, float expected[]) {
		float errors[] = null;

		for (int layer = nLayers; 0 < layer; layer--) {
			LayerWeight lw0 = lws.get(layer - 1);
			LayerWeight lw1 = layer < lws.size() ? lws.get(layer) : null;

			float ins[] = activations.get(layer - 1);
			float outs[] = activations.get(layer);
			float diffs[] = new float[lw0.nOutputs];

			if (lw1 != null)
				for (int j = 0; j < lw1.nInputs; j++) {
					float sum = 0f;
					for (int k = 0; k < lw1.nOutputs; k++)
						sum += errors[k] * lw1.weights[j][k];
					diffs[j] = sum;
				}
			else
				for (int j = 0; j < lw0.nOutputs; j++)
					diffs[j] = expected[j] - outs[j];

			float errors1[] = new float[lw0.nOutputs];

			for (int j = 0; j < lw0.nOutputs; j++) {
				errors1[j] = diffs[j] * activationFunctionGradient(outs[j]);
				for (int i = 0; i < lw0.nInputs; i++)
					lw0.weights[i][j] += learningRate * errors1[j] * ins[i];
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
