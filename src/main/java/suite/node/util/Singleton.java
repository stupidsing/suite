package suite.node.util;

import suite.inspect.Inspect;
import suite.inspect.Mapify;
import suite.os.StoreCache;
import suite.util.Nodify;

public class Singleton {

	public static final Singleton me = new Singleton();

	private Context grandContext = new Context();
	private Inspect inspect = new Inspect();
	private Mapify mapify = new Mapify(inspect);
	private Nodify nodify = new Nodify(inspect);
	private StoreCache storeCache = new StoreCache();

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

	public Nodify getNodify() {
		return nodify;
	}

	public StoreCache getStoreCache() {
		return storeCache;
	}

}
