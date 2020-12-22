package suite.lp.doer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Rewrite_;

public class Generalizer {

	private String variablePrefix;
	private Map<Atom, Reference> variables = new HashMap<>();

	public Generalizer() {
		this(ProverConstant.variablePrefix);
	}

	public Generalizer(String variablePrefix) {
		this.variablePrefix = variablePrefix;
	}

	public Node generalize(Node node) {
		Tree tree = Tree.of(null, null, node);
		generalizeRight(tree);
		return tree.getRight();
	}

	private void generalizeRight(Tree tree) {
		while (tree != null) {
			Tree nextTree = null;
			var right = tree.getRight();
			Tree rt;

			if (right instanceof Atom) {
				var atom = (Atom) right;
				var name = atom.name;
				if (name.startsWith(ProverConstant.wildcardPrefix))
					right = new Reference();
				if (name.startsWith(variablePrefix))
					right = getVariable(atom);
			} else if ((rt = Tree.decompose(right)) != null)
				right = nextTree = Tree.of(rt.getOperator(), generalize(rt.getLeft()), rt.getRight());
			else
				right = Rewrite_.map(right, this::generalize);

			Tree.forceSetRight(tree, right);
			tree = nextTree;
		}
	}

	public Reference getVariable(Atom variable) {
		return variables.computeIfAbsent(variable, any -> new Reference());
	}

	public Set<Atom> getVariableNames() {
		return variables.keySet();
	}

}
