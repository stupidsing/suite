package org.suite.doer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Tree;

public class Generalizer {

	public static final String defaultPrefix = ".";

	private static final Node WILDCARD = Atom.create("_");
	private static final Node CUT = Atom.create("!");

	private String variablePrefix = defaultPrefix;
	private Map<Node, Reference> variables = new HashMap<>();
	private Node cut;

	public Node generalize(Node node) {
		node = node.finalNode();

		if (isWildcard(node))
			return new Reference();
		else if (isVariable(node)) {
			Reference reference;
			if (!variables.containsKey(node)) {
				reference = new Reference();
				variables.put(node, reference);
			} else
				reference = variables.get(node);
			return reference;
		} else if (isCut(node) && cut != null) // Substitutes cut symbol to cut
			return cut;
		else if (node instanceof Tree) {
			Tree t = (Tree) node;
			Node l = t.getLeft(), r = t.getRight();
			Node gl = generalize(l), gr = generalize(r);
			if (gl != l || gr != r)
				return Tree.create(t.getOperator(), gl, gr);
		}

		return node;
	}

	public String dumpVariables() {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		List<Entry<Node, Reference>> entries;

		entries = new ArrayList<>(variables.entrySet());

		Collections.sort(entries, new Comparator<Entry<Node, Reference>>() {
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
