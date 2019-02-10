package suite.node.util;

import suite.inspect.Inspect;
import suite.inspect.Mapify;
import suite.os.StoreCache;
import suite.serialize.Serialize;
import suite.util.Nodify;

public class Singleton {

	public static final Singleton me = new Singleton();

	public final AtomContext atomContext = new AtomContext();
	public final Inspect inspect = new Inspect();
	public final Mapify mapify = new Mapify(inspect);
	public final Nodify nodify = new Nodify(inspect);
	public final Serialize serialize = new Serialize(inspect);
	public final StoreCache storeCache = new StoreCache();

	private Singleton() {
	}

}
