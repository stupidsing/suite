package org.suite.node;

import java.lang.ref.WeakReference;
import java.util.Map;

import org.suite.Context;
import org.suite.Singleton;

public class Atom extends Node {

	private String name;

	public final static Atom nil = create("");

	private Atom(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public static Atom create(String name) {
		return create(Singleton.get().getGrandContext(), name);
	}

	public static Atom create(Context context, String name) {
		Atom atom;
		Map<String, WeakReference<Atom>> pool = context.getAtomPool();
		synchronized (pool) {
			WeakReference<Atom> ref = pool.get(name);
			if (ref == null || (atom = ref.get()) == null)
				pool.put(name, new WeakReference<Atom>(atom = new Atom(name)));
		}
		return atom;
	}

	public String getName() {
		return name;
	}

}
