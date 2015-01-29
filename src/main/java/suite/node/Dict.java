package suite.node;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import suite.util.Util;

public class Dict extends Node {

	public final Map<Node, Node> map = new HashMap<>();

	@Override
	public int hashCode() {
		int result = 0;
		for (Entry<Node, Node> e : map.entrySet())
			result ^= 31 * e.getKey().hashCode() + e.getValue().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Node) {
			Node node = ((Node) object).finalNode();
			return Util.clazz(node) == Dict.class ? map.equals(((Dict) node).map) : false;
		} else
			return false;
	}

}
