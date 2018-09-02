package suite.concurrent;

import java.util.Random;

import suite.util.Thread_;

public class Backoff {

	private static Random random = new Random();
	private int duration = 50;

	public boolean exponentially() {
		Thread_.sleepQuietly(duration());
		return false;
	}

	public int duration() {
		var duration0 = duration;
		duration = duration * 5 / 4 + random.nextInt(50);
		return duration0;
	}

	public void yield() {
		Thread.yield();
	}

}
