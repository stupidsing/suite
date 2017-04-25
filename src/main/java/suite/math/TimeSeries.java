package suite.math;

import java.util.Arrays;

public class TimeSeries {

	private Matrix mtx = new Matrix();

	public float[] differences(float[] fs, int tor) {
		return differencesOn(mtx.of(fs), tor);
	}

	public float[] differencesOn(float[] fs, int tor) {
		int i = fs.length;
		while (tor <= --i)
			fs[i] -= fs[i - tor];
		while (0 <= --i)
			fs[i] = 0f;
		return fs;
	}

	public float[] drop(float[] fs, int tor) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	public float[][] drop(float[][] fs, int tor) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	public float[] dropDiff(float[] logs, int tor) {
		return drop(differences(logs, tor), tor);
	}

}
