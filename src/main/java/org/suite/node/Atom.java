package org.suite.node;

import java.lang.ref.WeakReference;
import java.util.Map;

import org.suite.Context;
import org.suite.Singleton;
import org.util.Util;

public class Atom extends Node {

	private String name;

	public static final Atom NIL = create("");
	public static final Atom TRUE = create("true");
	public static final Atom FALSE = create("false");

	private Atom(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (this != object)
			if (object instanceof Node) {
				Node node = ((Node) object).finalNode();
				if (node instanceof Atom) {
					Atom a = (Atom) node;
					return Util.equals(name, a.name);
				} else
					return false;
			} else
				return false;
		else
			return true;
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
				pool.put(name, new WeakReference<>(atom = new Atom(name)));
		}
		return atom;
	}

	public String getName() {
		return name;
	}

}
