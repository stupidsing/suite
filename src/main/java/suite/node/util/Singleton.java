package suite.node.util;

import suite.inspect.Inspect;
import suite.inspect.Mapify;

public class Singleton {

	private static Singleton instance = new Singleton();

	private Context grandContext = new Context();
	private Inspect inspect = new Inspect();
	private Mapify mapify = new Mapify(inspect);

	public static Singleton get() {
		return instance;
	}

	private Singleton() {
	}

	public Context getGrandContext() {
		return grandContext;
	}

	public Inspect getInspect() {
		return inspect;
	}

	public Mapify getMapify() {
		return mapify;
	}

}
