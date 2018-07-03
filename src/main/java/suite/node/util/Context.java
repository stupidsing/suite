package suite.node.util;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import suite.node.Atom;
import suite.streamlet.FunUtil.Fun;

public class Context {

	private Map<String, WeakReference<Atom>> atomPool = new WeakHashMap<>();

	public synchronized Atom findAtom(String key, Fun<String, Atom> fun) {
		Atom atom;
		WeakReference<Atom> ref = atomPool.get(key);
		if (ref == null || (atom = ref.get()) == null)
			atomPool.put(key, new WeakReference<>(atom = fun.apply(key)));
		return atom;
	}

}
