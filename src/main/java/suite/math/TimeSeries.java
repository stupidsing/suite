package suite.math;

import java.util.Arrays;

public class TimeSeries {

	private Matrix mtx = new Matrix();

	public float[] differences(float[] fs, int tor) {
		return differences_(fs, tor);
	}

	public float[] differencesOn(float[] fs, int tor) {
		return differencesOn_(fs, tor);
	}

	public float[] drop(float[] fs, int tor) {
		return drop_(fs, tor);
	}

	public float[][] drop(float[][] fs, int tor) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	public float[] dropDiff(float[] logs, int tor) {
		return drop_(differences_(logs, tor), tor);
	}

	private float[] drop_(float[] fs, int tor) {
		return Arrays.copyOfRange(fs, tor, fs.length);
	}

	private float[] differences_(float[] fs, int tor) {
		return differencesOn_(mtx.of(fs), tor);
	}

	private float[] differencesOn_(float[] fs, int tor) {
		int i = fs.length;
		while (tor <= --i)
			fs[i] -= fs[i - tor];
		while (0 <= --i)
			fs[i] = 0f;
		return fs;
	}

}
