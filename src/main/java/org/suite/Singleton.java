package org.suite;

public class Singleton {

	private static Singleton instance = new Singleton();

	private Context grandContext = new Context();
	private Context hiddenContext = new Context(); // For hidden symbols

	public static Singleton get() {
		return instance;
	}

	private Singleton() {
	}

	public Context getGrandContext() {
		return grandContext;
	}

	public Context getHiddenContext() {
		return hiddenContext;
	}

}
