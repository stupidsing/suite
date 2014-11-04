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

	public static <T> Pair<Sink<T>, Source<List<T>>> list() {
		List<T> list = new ArrayList<>();
		return Pair.of(list::add, () -> list);
	}

	public static <K, V> Pair<Sink<Pair<K, V>>, Source<Map<K, List<V>>>> listMap() {
		Map<K, List<V>> map = new HashMap<>();
		return Pair.of(pair -> map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1), () -> map);
	}

	public static <K, V> Pair<Sink<Pair<K, V>>, Source<Map<K, V>>> map() {
		Map<K, V> map = new HashMap<>();
		return Pair.of(pair -> map.put(pair.t0, pair.t1), () -> map);
	}

	public static <T> Pair<Sink<T>, Source<Set<T>>> set() {
		Set<T> set = new HashSet<>();
		return Pair.of(set::add, () -> set);
	}

}
