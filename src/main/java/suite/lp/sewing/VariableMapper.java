package suite.lp.sewing;

import suite.lp.doer.ProverConstant;
import suite.node.Atom;
import suite.node.Node;

public interface VariableMapper {

	public Env env();

	/**
	 * Would a certain end-node be generalized?
	 */
	public static boolean isVariant(Node node) {
		if (node instanceof Atom) {
			String name = ((Atom) node).name;
			return isWildcard(name) || isVariable(name) || isCut(node);
		} else
			return false;
	}

	public static boolean isWildcard(String name) {
		return name.startsWith(ProverConstant.wildcardPrefix);
	}

	public static boolean isVariable(String name) {
		return name.startsWith(ProverConstant.variablePrefix);
	}

	public static boolean isCut(Node node) {
		return node == ProverConstant.cut;
	}

}
