package suite.node;

import suite.node.util.Context;
import suite.node.util.Singleton;
import suite.util.Util;

public class Atom extends Node {

	public final String name;

	public static Atom NIL = of("");
	public static Atom NULL = of("null");
	public static Atom TRUE = of("true");
	public static Atom FALSE = of("false");

	public static String name(Node node) {
		return ((Atom) node).name;
	}

	public static Atom of(String name) {
		return of(Singleton.me.grandContext, name);
	}

	public static Atom of(Context context, String name) {
		return context.findAtom(name, Atom::new);
	}

	private Atom(String name) {
		this.name = name;
	}

	public static Atom temp() {
		return Atom.of("temp$$" + Util.temp());
	}

}
