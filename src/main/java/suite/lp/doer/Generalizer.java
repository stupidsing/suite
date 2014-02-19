package suite.lp.doer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.node.Atom;
import suite.node.Lazy;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermParser.TermOp;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class Generalizer {

	public static final String defaultPrefix = ".";

	private static final Node WILDCARD = Atom.create("_");
	private static final Node CUT = Atom.create("!");

	private String variablePrefix = defaultPrefix;
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

			if (isWildcard(right))
				right = new Reference();
			else if (isVariable(right)) {
				Reference reference = variables.get(right);

				if (reference == null)
					variables.put(right, reference = new Reference());

				right = reference;
			} else if (isCut(right) && cut != null) // Changes cut symbol to cut
				right = cut;
			else if (right instanceof Tree) {
				final Tree rightTree = (Tree) right;
				final Operator rightOp = rightTree.getOperator();

				// Delay generalizing for performance
				if (rightOp == TermOp.OR____)
					right = new Lazy(new Source<Node>() {
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
		return isWildcard(node) || isVariable(node) || isCut(node);
	}

	private static boolean isWildcard(Node node) {
		return node == WILDCARD;
	}

	private boolean isVariable(Node node) {
		return node instanceof Atom && ((Atom) node).getName().startsWith(variablePrefix);
	}

	private static boolean isCut(Node node) {
		return node == CUT;
	}

	public Reference getVariable(Node name) {
		return variables.get(name);
	}

	public void setVariablePrefix(String variablePrefix) {
		this.variablePrefix = variablePrefix;
	}

	public void setCut(Node cut) {
		this.cut = cut;
	}

}
