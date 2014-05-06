package suite.node.util;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import suite.node.Atom;

public class Context {

	private Map<String, WeakReference<Atom>> atomPool = new WeakHashMap<>();

	public void gc() {
		Collection<WeakReference<Atom>> values = atomPool.values();
		Iterator<WeakReference<Atom>> iter = values.iterator();
		while (iter.hasNext())
			if (iter.next().get() == null)
				iter.remove();
	}

	public Map<String, WeakReference<Atom>> getAtomPool() {
		return atomPool;
	}

}
