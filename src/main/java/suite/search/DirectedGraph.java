package suite.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class DirectedGraph<V> {

	public final Set<V> vertices;
	public final ListMultimap<V, V> forwards;
	public final ListMultimap<V, V> backwards;

	public static <V> DirectedGraph<V> of(Set<Pair<V, V>> edges) {
		Streamlet<V> vertices0 = Read.from(edges).map(edge -> edge.t0);
		Streamlet<V> vertices1 = Read.from(edges).map(edge -> edge.t1);
		Set<V> vertices = Streamlet.concat(vertices0, vertices1).toSet();
		return DirectedGraph.of(vertices, edges);
	}

	public static <V> DirectedGraph<V> of(Set<V> vertices, Set<Pair<V, V>> edges) {
		ListMultimap<V, V> forwards = Read.from(edges).toMultimap(Pair::first_, Pair::second);
		ListMultimap<V, V> backwards = Read.from(edges).toMultimap(Pair::second, Pair::first_);
		return new DirectedGraph<>(vertices, forwards, backwards);
	}

	private DirectedGraph(Set<V> vertices, ListMultimap<V, V> forwards, ListMultimap<V, V> backwards) {
		this.vertices = vertices;
		this.forwards = forwards;
		this.backwards = backwards;
	}

	public List<Set<V>> layers() {
		ArrayList<Set<V>> results = new ArrayList<>();
		Set<V> set = new HashSet<>();
		boolean b;

		do {
			Set<V> set1 = new HashSet<>();
			for (V v : vertices)
				if (!set.contains(v) && Read.from(forwards.get(v)).isAll(set::contains))
					set1.add(v);
			results.add(set1);
			set.addAll(set1);
			b = !set1.isEmpty();
		} while (b);

		if (set.size() == vertices.size())
			return results;
		else
			throw new RuntimeException("cyclic graph");
	}

	public DirectedGraph<V> reverse() {
		return new DirectedGraph<>(vertices, backwards, forwards);
	}

}
