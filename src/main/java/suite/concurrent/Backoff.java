package suite.concurrent;

import java.util.Random;

import suite.util.Thread_;

public class Backoff {

	private static Random random = new Random();
	private int duration = 50;

	public boolean exponentially() {
		Thread_.sleepQuietly(duration);
		duration = duration * 5 / 4 + random.nextInt(50);
		return false;
	}

	public void yield() {
		Thread.yield();
	}

}
