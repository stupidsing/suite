package suite.search;

import static suite.util.Friends.fail;

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
		var vertices0 = Read.from(edges).map(edge -> edge.k);
		var vertices1 = Read.from(edges).map(edge -> edge.v);
		Set<V> vertices = Streamlet.concat(vertices0, vertices1).toSet();
		return DirectedGraph.of(vertices, edges);
	}

	public static <V> DirectedGraph<V> of(Set<V> vertices, Set<Pair<V, V>> edges) {
		var forwards = Read.from(edges).toMultimap(Pair::fst, Pair::snd);
		var backwards = Read.from(edges).toMultimap(Pair::snd, Pair::fst);
		return new DirectedGraph<>(vertices, forwards, backwards);
	}

	private DirectedGraph(Set<V> vertices, ListMultimap<V, V> forwards, ListMultimap<V, V> backwards) {
		this.vertices = vertices;
		this.forwards = forwards;
		this.backwards = backwards;
	}

	public List<Set<V>> layers() {
		var results = new ArrayList<Set<V>>();
		var set = new HashSet<V>();
		boolean b;

		do {
			var set1 = new HashSet<V>();
			for (var v : vertices)
				if (!set.contains(v) && Read.from(forwards.get(v)).isAll(set::contains))
					set1.add(v);
			results.add(set1);
			set.addAll(set1);
			b = !set1.isEmpty();
		} while (b);

		return set.size() == vertices.size() ? results : fail("cyclic graph");
	}

	public DirectedGraph<V> reverse() {
		return new DirectedGraph<>(vertices, backwards, forwards);
	}

}
