package suite.node;

import java.util.HashMap;
import java.util.Map;

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
		return map.hashCode();
	}

}
