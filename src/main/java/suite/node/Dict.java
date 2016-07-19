package suite.node;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import suite.util.Util;

public class Dict extends Node {

	public final Map<Node, Reference> map;

	public Dict() {
		this(new HashMap<>());
	}

	public Dict(Map<Node, Reference> map) {
		this.map = map;
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == Dict.class ? map.equals(((Dict) object).map) : false;
	}

	@Override
	public int hashCode() {
		int result = 0;
		for (Entry<Node, Reference> e : map.entrySet())
			result ^= 31 * e.getKey().hashCode() + e.getValue().hashCode();
		return result;
	}

}
