package suite.node.util;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import primal.fp.Funs.Fun;
import suite.node.Atom;

public class AtomContext {

	private Map<String, WeakReference<Atom>> atomPool = new WeakHashMap<>();

	public synchronized Atom findAtom(String key, Fun<String, Atom> fun) {
		var ref = atomPool.get(key);
		Atom atom;
		if (ref == null || (atom = ref.get()) == null)
			atomPool.put(key, new WeakReference<>(atom = fun.apply(key)));
		return atom;
	}

}
