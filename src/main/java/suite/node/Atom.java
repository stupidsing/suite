package suite.node;

import java.util.concurrent.atomic.AtomicInteger;

import suite.node.util.Context;
import suite.node.util.Singleton;

public class Atom extends Node {

	private String name;

	public static Atom NIL = of("");
	public static Atom TRUE = of("true");
	public static Atom FALSE = of("false");

	private static AtomicInteger counter = new AtomicInteger();

	private Atom(String name) {
		this.name = name;
	}

	public static Atom of(String name) {
		return of(Singleton.get().getGrandContext(), name);
	}

	public static Atom of(Context context, String name) {
		return context.findAtom(name, Atom::new);
	}

	public static Atom temp() {
		return Atom.of("temp$$" + counter.getAndIncrement());
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object object) {
		return this == object //
				|| object instanceof Node && this == ((Node) object).finalNode();
	}

	public String getName() {
		return name;
	}

}
