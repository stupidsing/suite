package suite.streamlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class As {

	public static <T> Pair<Sink<Streamlet<T>>, Source<Streamlet<T>>> concat() {
		List<T> list = new ArrayList<>();
		return Pair.of(st -> st.sink(t -> list.add(t)), () -> Read.from(list));
	}

	public static Pair<Sink<Integer>, Source<int[]>> intArray() {
		List<Integer> list = new ArrayList<>();
		return Pair.of(list::add, () -> {
			int size = list.size();
			int results[] = new int[size];
			for (int i = 0; i < size; i++)
				results[i] = list.get(i);
			return results;
		});
	}

	public static Pair<Sink<String>, Source<String>> joined() {
		return joined("");
	}

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

	public static <K, V> Pair<Sink<Pair<K, List<V>>>, Source<ListMultimap<K, V>>> multimap() {
		Pair<Sink<Pair<K, List<V>>>, Source<Map<K, List<V>>>> p = map();
		return Pair.of(p.t0, () -> new ListMultimap<>(p.t1.source()));
	}

	public static <K, V> Pair<Sink<Pair<K, V>>, Source<Map<K, Set<V>>>> setMap() {
		Map<K, Set<V>> map = new HashMap<>();
		return Pair.of(pair -> map.computeIfAbsent(pair.t0, k_ -> new HashSet<>()).add(pair.t1), () -> map);
	}

}
