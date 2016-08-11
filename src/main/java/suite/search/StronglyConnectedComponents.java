package suite.search;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.streamlet.Read;

/**
 * http://en.wikipedia.org/wiki/Tarjan%27
 * s_strongly_connected_components_algorithm
 * 
 * @author ywsing
 */
public class StronglyConnectedComponents<V> {

	private DirectedGraph<V> dg;
	private ListMultimap<Scc, Scc> forwards = new ListMultimap<>();
	private int index = 0;
	private Deque<Scc> stack = new ArrayDeque<>();

	public final List<Set<V>> components = new ArrayList<>();

	private class Scc {
		private V v;
		private boolean isVisited;
		private boolean isStacked;
		private int index;
		private int lowestLink;

		private Scc(V v) {
			this.v = v;
		}
	}

	public StronglyConnectedComponents(DirectedGraph<V> dg) {
		this.dg = dg;

		Map<V, Scc> sccs = Read.from(dg.vertices).map2(v -> v, v -> new Scc(v)).toMap();
		forwards = dg.forwards.listEntries().mapEntry((u, v) -> sccs.get(u), (u, v) -> sccs.get(v)).toMultimap();

		for (Scc vscc : sccs.values())
			if (!vscc.isVisited)
				strongConnect(vscc);
	}

	private void strongConnect(Scc vscc) {
		vscc.isVisited = vscc.isStacked = true;
		vscc.index = vscc.lowestLink = index++;
		stack.push(vscc);

		for (Scc wscc : forwards.get(vscc))
			if (!wscc.isVisited) {
				strongConnect(wscc);
				vscc.lowestLink = Math.min(vscc.lowestLink, wscc.lowestLink);
			} else if (wscc.isStacked)
				vscc.lowestLink = Math.min(vscc.lowestLink, wscc.index);

		if (vscc.index == vscc.lowestLink) {
			Set<V> set = new HashSet<>();
			Scc wscc;
			do {
				(wscc = stack.pop()).isStacked = false;
				set.add(wscc.v);
			} while (wscc != vscc);
			components.add(set);
		}
	}

	public DirectedGraph<Set<V>> group() {
		Map<V, Set<V>> map = Read.from(components).concatMap2(vs -> Read.from(vs).map2(v -> v, v -> vs)).toMap();
		Set<Set<V>> vertices = Read.from(components).toSet();
		Set<Pair<Set<V>, Set<V>>> edges = new HashSet<>();

		for (V v0 : dg.vertices)
			for (V v1 : dg.forwards.get(v0)) {
				Set<V> vs0 = map.get(v0);
				Set<V> vs1 = map.get(v1);
				if (vs0 != vs1)
					edges.add(Pair.of(vs0, vs1));
			}

		return DirectedGraph.of(vertices, edges);
	}

}
