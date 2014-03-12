package suite.lp.doer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class Generalizer {

	private static final String wildcardPrefix = "_";
	public static final String variablePrefix = ".";
	private static final String cutName = "!";

	private Map<Node, Reference> variables = new HashMap<>();
	private Node cut;

	public Node generalize(Node node) {
		Tree tree = Tree.create(null, null, node);
		generalizeRight(tree);
		return tree.getRight();
	}

	private void generalizeRight(Tree tree) {
		while (true) {
			Node right = tree.getRight().finalNode();

			if (right instanceof Atom) {
				String name = ((Atom) right).getName();
				if (isWildcard(name))
					right = new Reference();
				else if (isVariable(name))
					right = getVariable(right);
				else if (isCut(name) && cut != null)
					right = cut;
			} else if (right instanceof Tree) {
				final Tree rightTree = (Tree) right;
				final Operator rightOp = rightTree.getOperator();

				// Delay generalizing for performance
				if (rightOp == TermOp.OR____)
					right = new Suspend(new Source<Node>() {
						public Node source() {
							Node rl = rightTree.getLeft();
							Node rr = rightTree.getRight();
							return Tree.create(rightOp, generalize(rl), generalize(rr));
						}
					});
				else {
					Tree rightTree1 = Tree.create(rightOp, generalize(rightTree.getLeft()), rightTree.getRight());
					Tree.forceSetRight(tree, rightTree1);
					tree = rightTree1;
					continue;
				}
			}

			Tree.forceSetRight(tree, right);
			break;
		}
	}

	public Reference getVariable(Node variable) {
		Reference reference = variables.get(variable);
		if (reference == null)
			variables.put(variable, reference = new Reference());
		return reference;
	}

	public String dumpVariables() {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		List<Entry<Node, Reference>> entries = Util.sort(variables.entrySet(), new Comparator<Entry<Node, Reference>>() {
			public int compare(Entry<Node, Reference> e0, Entry<Node, Reference> e1) {
				return e0.getKey().compareTo(e1.getKey());
			}
		});

		for (Entry<Node, Reference> entry : entries) {
			if (first)
				first = false;
			else
				sb.append(", ");

			sb.append(Formatter.dump(entry.getKey()));
			sb.append(" = ");
			sb.append(Formatter.dump(entry.getValue()));
		}

		return sb.toString();
	}

	/**
	 * Would a certain end-node be generalized?
	 */
	public boolean isVariant(Node node) {
		node = node.finalNode();
		if (node instanceof Atom) {
			String name = ((Atom) node).getName();
			return isWildcard(name) || isVariable(name) || isCut(name);
		} else
			return false;
	}

	private static boolean isWildcard(String name) {
		return name.startsWith(wildcardPrefix);
	}

	private boolean isVariable(String name) {
		return name.startsWith(variablePrefix);
	}

	private static boolean isCut(String name) {
		return name.equals(cutName);
	}

	public void setCut(Node cut) {
		this.cut = cut;
	}

}
