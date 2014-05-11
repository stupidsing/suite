package suite.node.util;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import suite.node.Atom;
import suite.util.FunUtil.Source;

public class Context {

	private Map<String, WeakReference<Atom>> atomPool = new WeakHashMap<>();

	public synchronized Atom findAtom(String key, Source<Atom> source) {
		Atom atom;
		WeakReference<Atom> ref = atomPool.get(key);
		if (ref == null || (atom = ref.get()) == null)
			atomPool.put(key, new WeakReference<>(atom = source.source()));
		return atom;
	}

}
