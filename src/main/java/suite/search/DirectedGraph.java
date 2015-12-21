package suite.search;

import java.util.List;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.streamlet.Read;

public class DirectedGraph<V> {

	public final List<V> vertices;
	public final ListMultimap<V, V> forwards;
	public final ListMultimap<V, V> backwards;

	public DirectedGraph(List<V> vertices, List<Pair<V, V>> edges) {
		this.vertices = vertices;
		forwards = Read.from(edges).toMultimap(Pair::first_, Pair::second);
		backwards = Read.from(edges).toMultimap(Pair::second, Pair::first_);
	}

}
