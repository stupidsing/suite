package suite.streamlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Pair;

public class As {

	public static Pair<Sink<String>, Source<String>> joined(String delimiter) {
		return joined("", delimiter, "");
	}

	public static Pair<Sink<String>, Source<String>> joined(String before, String delimiter, String after) {
		StringBuilder sb = new StringBuilder();
		sb.append(before);
		return Pair.of(new Sink<String>() {
			private boolean first = true;

			public void sink(String s) {
				if (first)
					first = false;
				else
					sb.append(delimiter);
				sb.append(s);
			}
		}, () -> {
			sb.append(after);
			return sb.toString();
		});
	}

	public static <K, V> Pair<Sink<Pair<K, V>>, Source<Map<K, List<V>>>> listMap() {
		Map<K, List<V>> map = new HashMap<>();
		return Pair.of(pair -> map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1), () -> map);
	}

	public static <K, V> Pair<Sink<Pair<K, V>>, Source<Map<K, V>>> map() {
		Map<K, V> map = new HashMap<>();
		return Pair.of(pair -> map.put(pair.t0, pair.t1), () -> map);
	}

	public static <K, V> Pair<Sink<Pair<K, V>>, Source<Map<K, Set<V>>>> setMap() {
		Map<K, Set<V>> map = new HashMap<>();
		return Pair.of(pair -> map.computeIfAbsent(pair.t0, k_ -> new HashSet<>()).add(pair.t1), () -> map);
	}

}
