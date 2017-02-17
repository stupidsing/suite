package suite.node;

import suite.lp.doer.ProverConstant;
import suite.util.Util;

public class Reference extends Node {

	private Node node = this;
	private int id = Util.temp();

	public static Reference of(Node node) {
		Reference reference = new Reference();
		reference.bound(node);
		return reference;
	}

	public String name() {
		return ProverConstant.variablePrefix + id;
	}

	public boolean isFree() {
		return finalNode() instanceof Reference;
	}

	public void bound(Node node) {
		this.node = node;
	}

	public void unbound() { // happens during backtracking
		node = this;
	}

	@Override
	public Node finalNode() {
		return node != this ? node.finalNode() : node;
	}

	@Override
	public boolean equals(Object object) {
		if (node != this)
			return node.equals(object);
		else
			throw new RuntimeException("No equals for free references");
	}

	@Override
	public int hashCode() {
		if (node != this)
			return node.hashCode();
		else
			throw new RuntimeException("No hash code for free references");
	}

	public Node getNode() {
		return node;
	}

	public int getId() {
		return id;
	}

}
