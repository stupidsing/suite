package suite.node;

import java.util.HashMap;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.util.Object_;

public class Dict extends Node {

	public final Map<Node, Reference> map;

	public static Dict of(Pair<Node, Reference>[] pairs) {
		Map<Node, Reference> map = new HashMap<>();
		for (Pair<Node, Reference> pair : pairs)
			map.put(pair.t0, pair.t1);
		return new Dict(map);
	}

	public Dict() {
		this(new HashMap<>());
	}

	public Dict(Map<Node, Reference> map) {
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
