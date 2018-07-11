package suite.node; import static suite.util.Friends.fail;

import suite.lp.doer.ProverConstant;
import suite.util.Util;

public class Reference extends Node {

	private Node node = this;
	private int id = Util.temp();

	public static Reference of(Node node) {
		var reference = new Reference();
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
		return node != this ? node.equals(object) : fail("no equals for free references");
	}

	@Override
	public int hashCode() {
		return node != this ? node.hashCode() : fail("no hash code for free references");
	}

	public int getId() {
		return id;
	}

}
