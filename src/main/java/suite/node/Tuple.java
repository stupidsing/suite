package suite.node;

import java.util.Objects;

import suite.util.Util;

public class Tuple extends Node {

	public final Node[] nodes;

	public static Tuple of(Node[] nodes) {
		return new Tuple(nodes);
	}

	private Tuple(Node[] nodes) {
		this.nodes = nodes;
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == Tuple.class && Objects.equals(nodes, ((Tuple) object).nodes);
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (Node node : nodes)
			result = 31 * result + node.hashCode();
		return result;
	}

}
