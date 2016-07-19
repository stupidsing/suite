package suite.node;

import java.util.List;
import java.util.Objects;

import suite.util.Util;

public class Tuple extends Node {

	public final List<Node> nodes;

	public static Tuple of(List<Node> nodes) {
		return new Tuple(nodes);
	}

	private Tuple(List<Node> nodes) {
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
