package suite.lp.doer;

import suite.node.Atom;
import suite.node.Node;

public class ProverConstant {

	public static String wildcardPrefix = "_";
	public static String variablePrefix = ".";
	public static String cutName = "!";
	public static Atom cut = Atom.of(cutName);

	/**
	 * Would a certain end-node be generalized?
	 */
	public static boolean isVariant(Node node) {
		if (node instanceof Atom atom) {
			var name = atom.name;
			return isWildcard(name) || isVariable(name) || isCut(node);
		} else
			return false;
	}

	public static boolean isCut(Node node) {
		return node == ProverConstant.cut;
	}

	public static boolean isVariable(String name) {
		return name.startsWith(ProverConstant.variablePrefix);
	}

	public static boolean isWildcard(String name) {
		return name.startsWith(ProverConstant.wildcardPrefix);
	}

}
