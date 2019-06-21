package suite.math.numeric;

import suite.math.Sigmoid;
import suite.math.linalg.Vector;
import suite.streamlet.FunUtil.Fun;

/**
 * https://medium.com/@edwinvarghese4442/logistic-regression-with-gradient-descent-tutorial-part-1-theory-529c93866001
 * 
 * https://medium.com/@edwinvarghese4442/logistic-regression-with-gradient-descent-tutorial-part-2-code-a4544bb1505
 *
 * @author ywsing
 */
public class LogisticRegression {

	private Vector vec = new Vector();

	public Fun<float[], Double> train(float[][] xs, float[] ys) {
		var learningRate = .01d;
		var nSamples = xs.length;
		var nParameters = xs[0].length;

		var weights = new float[nParameters];
		var b = 0d;

		for (var iter = 0; iter < 2048; iter++) {
			var errorSum = 0d;
			var errors = new float[nParameters];

			for (var i = 0; i < nSamples; i++) {
				var output = b + vec.dot(weights, xs[i]);
				var error = Sigmoid.sigmoid(output) - ys[i];
				errorSum += error;
				vec.addOn(errors, vec.scale(xs[i], error));
			}

			var inv = 1d / nSamples;
			b -= learningRate * errorSum * inv;
			vec.subOn(weights, vec.scaleOn(errors, learningRate * inv));
		}

		var b_ = b;
		return xs_ -> Sigmoid.sigmoid(vec.dot(weights, xs_) + b_);
	}

}
