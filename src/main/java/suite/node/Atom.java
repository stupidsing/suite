package suite.node;

import primal.Verbs.Get;
import suite.node.util.Singleton;

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
		return Singleton.me.atomContext.findAtom(name, Atom::new);
	}

	private Atom(String name) {
		this.name = name;
	}

	public static Atom temp() {
		return Atom.of("a$" + (Get.temp() + 1000000000));
	}

}
