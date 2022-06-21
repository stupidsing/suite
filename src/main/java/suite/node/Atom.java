package suite.node;

import primal.Verbs.Get;
import suite.node.util.AtomContext;

public class Atom extends Node {

	public static AtomContext atomContext = new AtomContext();
	public static Atom NIL = of("");
	public static Atom NULL = of("null");
	public static Atom TRUE = of("true");
	public static Atom FALSE = of("false");

	public final String name;

	public static String name(Node node) {
		return ((Atom) node).name;
	}

	public static Atom of(String name) {
		return atomContext.findAtom(name, Atom::new);
	}

	private Atom(String name) {
		this.name = name;
	}

	public static Atom temp() {
		return Atom.of("a$" + Get.temp());
	}

}
