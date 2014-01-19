package suite.math;

public class XorShiftRandom {

	private long seed;

	public long nextLong() {
		seed ^= seed << 21;
		seed ^= seed >>> 35;
		seed ^= seed << 4;
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

}
