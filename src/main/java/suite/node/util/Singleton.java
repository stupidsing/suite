package suite.node.util;

import suite.inspect.Inspect;
import suite.inspect.Mapify;
import suite.os.StoreCache;
import suite.util.Nodify;

public class Singleton {

	public static final Singleton me = new Singleton();

	public final Context grandContext = new Context();
	public final Inspect inspect = new Inspect();
	public final Mapify mapify = new Mapify(inspect);
	public final Nodify nodify = new Nodify(inspect);
	public final StoreCache storeCache = new StoreCache();

	private Singleton() {
	}

}
