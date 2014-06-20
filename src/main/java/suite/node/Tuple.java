package suite.node;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import suite.util.Util;

public class Tuple extends Node {

	private List<Node> nodes;

	public Tuple(Node... nodes) {
		this.nodes = Arrays.asList(nodes);
	}

	public Tuple(List<Node> nodes) {
		this.nodes = nodes;
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (Node node : nodes)
			result = 31 * result + node.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Node) {
			Node node = ((Node) object).finalNode();
			return Util.clazz(node) == Tuple.class && Objects.equals(nodes, ((Tuple) node).nodes);
		} else
			return false;
	}

	public List<Node> getNodes() {
		return nodes;
	}

}
