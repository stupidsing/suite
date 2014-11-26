package suite.node;

import java.util.concurrent.atomic.AtomicInteger;

public class Reference extends Node {

	private Node node = this;
	private int id = counter.getAndIncrement();

	private static AtomicInteger counter = new AtomicInteger();

	public boolean isFree() {
		Node node = finalNode();
		return node instanceof Reference && ((Reference) node).node == node;
	}

	public void bound(Node node) {
		this.node = node;
	}

	public void unbound() { // Happens during backtracking
		node = this;
	}

	@Override
	public Node finalNode() {
		return node != this ? node.finalNode() : node;
	}

	@Override
	public int hashCode() {
		if (node != this)
			return node.hashCode();
		else
			throw new RuntimeException("No hash code for free references");
	}

	@Override
	public boolean equals(Object object) {
		if (node == this)
			return object instanceof Node && super.equals(((Node) object).finalNode());
		else
			return node.equals(object);
	}

	public int getId() {
		return id;
	}

}
