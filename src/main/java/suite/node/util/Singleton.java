package suite.node.util;

import suite.inspect.Inspect;
import suite.inspect.Mapify;
import suite.os.StoreCache;
import suite.serialize.Serialize;
import suite.util.Nodify;

public class Singleton {

	public static final Singleton me = new Singleton();

	public final Inspect inspect = Ioc.of(Inspect.class);
	public final Mapify mapify = Ioc.of(Mapify.class);
	public final Nodify nodify = Ioc.of(Nodify.class);
	public final Serialize serialize = Ioc.of(Serialize.class);
	public final StoreCache storeCache = Ioc.of(StoreCache.class);

	private Singleton() {
	}

}
