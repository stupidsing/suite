package suite.search;

import java.util.List;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;

public class DirectedGraph<V> {

	public List<V> vertices;
	public ListMultimap<V, V> forwards;
	public ListMultimap<V, V> backwards;

	public DirectedGraph(List<V> vertices, List<Pair<V, V>> edges) {
		this.vertices = vertices;
		forwards = Read.from(edges).groupBy(Pair::first_, Pair::second).collect(As.multimap());
		backwards = Read.from(edges).groupBy(Pair::second, Pair::first_).collect(As.multimap());
	}

}
