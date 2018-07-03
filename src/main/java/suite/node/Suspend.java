package suite.node;

import suite.streamlet.FunUtil.Source;

public class Suspend extends Node {

	private Source<Node> source;
	private Node target;

	public Suspend(Source<Node> source) {
		this.source = source;
	}

	@Override
	public Node finalNode() {
		return target().finalNode();
	}

	@Override
	public boolean equals(Object object) {
		return target().equals(object);
	}

	@Override
	public int hashCode() {
		return target().hashCode();
	}

	private Node target() {
		if (target == null)
			target = source.source();
		return target;
	}

}
