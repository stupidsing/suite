package suite.streamlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class As {

	public static <T> Streamlet<T> concat(Outlet<Streamlet<T>> outlet) {
		List<T> list = new ArrayList<>();
		outlet.sink(st1 -> st1.sink(list::add));
		return Read.from(list);
	}

	public static <K, V> Streamlet<Pair<K, Streamlet<V>>> groups(Outlet<Pair<K, V>> outlet) {
		Map<K, List<V>> map = new HashMap<>();
		outlet.sink(p -> map.computeIfAbsent(p.t0, k_ -> new ArrayList<>()).add(p.t1));
		return Read.from(map).map(Pair.map1(Read::from));
	}

	public static int[] intArray(Outlet<Integer> st) {
		List<Integer> list = new ArrayList<>();
		st.sink(list::add);

		int size = list.size();
		int results[] = new int[size];
		for (int i = 0; i < size; i++)
			results[i] = list.get(i);
		return results;
	}

	public static Fun<Outlet<String>, String> joined() {
		return joined("");
	}

	public static Fun<Outlet<String>, String> joined(String delimiter) {
		return joined("", delimiter, "");
	}

	public static Fun<Outlet<String>, String> joined(String before, String delimiter, String after) {
		return outlet -> {
			StringBuilder sb = new StringBuilder();
			sb.append(before);
			outlet.sink(new Sink<String>() {
				private boolean first = true;

				public void sink(String s) {
					if (first)
						first = false;
					else
						sb.append(delimiter);
					sb.append(s);
				}
			});
			sb.append(after);
			return sb.toString();
		};
	}

	public static <K, V> Map<K, List<V>> listMap(Outlet<Pair<K, V>> outlet) {
		Map<K, List<V>> map = new HashMap<>();
		outlet.sink(pair -> map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1));
		return map;
	}

	public static <K, V> Map<K, V> map(Outlet<Pair<K, V>> outlet) {
		Map<K, V> map = new HashMap<>();
		outlet.sink(pair -> map.put(pair.t0, pair.t1));
		return map;
	}

	public static <K, V> ListMultimap<K, V> multimap(Outlet<Pair<K, List<V>>> outlet) {
		return new ListMultimap<>(map(outlet));
	}

	public static <K, V> Map<K, Set<V>> setMap(Outlet<Pair<K, V>> outlet) {
		Map<K, Set<V>> map = new HashMap<>();
		outlet.sink(pair -> map.computeIfAbsent(pair.t0, k_ -> new HashSet<>()).add(pair.t1));
		return map;
	}

}
