package suite.ts;

import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;

public class Kalman {

	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	private int stateLength = 16;
	private float[][] F; // state transition (stateLength x stateLength)
	private float[][] B; // input control (stateLength x inputSize)
	private float[][] H; // observation (observeLength x stateLength)
	private float[][] Q; // state noise (stateLength x stateLength)
	private float[][] R; // observation noise (observeLength x observeLength)
	private float[][] estimatedStateCov0; // (stateLength x stateLength)
	private float[] estimatedState0; // estimated state (stateLength)

	// hidden equations:
	// state1 = F * state0 + B * input0 + noise(0, Q)
	// observation1 = H * state1 + noise(0, R)

	public void kalman(float[] input0, float[] observed0) {
		var identity = mtx.identity(stateLength);
		var Ft = mtx.transpose(F);
		var Ht = mtx.transpose(H);

		// predict
		// predicted next state (stateLength)
		// predicted next state covariance (stateLength x stateLength)
		var predictedState1 = vec.add(mtx.mul(F, estimatedState0), mtx.mul(B, input0));
		var predictedStateCov1 = mtx.add(mtx.mul(F, estimatedStateCov0, Ft), Q);

		// update
		// Kalman gain (stateLength x observeLength)
		// estimated next state (stateLength)
		// estimated next state covariance (stateLength x stateLength)
		var kalmanGain = mtx.mul(predictedStateCov1, Ht, mtx.inverse(mtx.add(R, mtx.mul(H, predictedStateCov1, Ht))));
		var estimatedState1 = vec.add(predictedState1, mtx.mul(kalmanGain, vec.sub(observed0, mtx.mul(H, predictedState1))));
		var estimatedStateCov1 = mtx.mul(mtx.sub(identity, mtx.mul(kalmanGain, H)), predictedStateCov1);
		// residual1 = observed0 - H * estimatedState1

		estimatedStateCov0 = estimatedStateCov1;
		estimatedState0 = estimatedState1;
	}

}
