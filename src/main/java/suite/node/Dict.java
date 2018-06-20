package suite.node;

import java.util.HashMap;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.util.Object_;

public class Dict extends Node {

	public final Map<Node, Reference> map;

	public static Map<Node, Reference> m(Node node) {
		return ((Dict) node).map;
	}

	public static Dict of() {
		return new Dict(new HashMap<>());
	}

	public static Dict ofPairs(Pair<Node, Reference>[] pairs) {
		var map = new HashMap<Node, Reference>();
		for (var pair : pairs)
			map.put(pair.t0, pair.t1);
		return of(map);
	}

	public static Dict of(Map<Node, Reference> map) {
		return new Dict(map);
	}

	private Dict() {
		this(new HashMap<>());
	}

	private Dict(Map<Node, Reference> map) {
		this.map = map;
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == Dict.class ? map.equals(((Dict) object).map) : false;
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

}
