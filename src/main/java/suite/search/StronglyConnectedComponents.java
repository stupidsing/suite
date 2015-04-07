package suite.search;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import suite.adt.IdentityKey;
import suite.streamlet.Read;

/**
 * http://en.wikipedia.org/wiki/Tarjan%27
 * s_strongly_connected_components_algorithm
 * 
 * @author ywsing
 */
public class StronglyConnectedComponents<V> {

	private DirectedGraph<V> dg;
	private int index = 0;
	private Deque<V> stack = new ArrayDeque<>();
	private Map<V, Scc> sccs = new HashMap<>();
	public final List<IdentityKey<Set<V>>> components = new ArrayList<>();

	private static class Scc {
		private boolean isVisited;
		private boolean isStacked;
		private int index;
		private int lowestLink;
	}

	public StronglyConnectedComponents(DirectedGraph<V> dg) {
		this.dg = dg;
		for (V v : dg.vertices)
			sccs.put(v, new Scc());

		tarjan();
	}

	public void tarjan() {
		for (V v : dg.vertices) {
			Scc vscc = sccs.get(v);
			if (!vscc.isVisited)
				strongConnect(v, vscc);
		}
	}

	private void strongConnect(V v, Scc vscc) {
		vscc.isVisited = vscc.isStacked = true;
		vscc.index = vscc.lowestLink = index++;
		stack.push(v);

		for (V w : dg.forwards.get(v)) {
			Scc wscc = sccs.get(w);
			if (!wscc.isVisited) {
				strongConnect(w, wscc);
				vscc.lowestLink = Math.min(vscc.lowestLink, wscc.lowestLink);
			} else if (wscc.isStacked)
				vscc.lowestLink = Math.min(vscc.lowestLink, wscc.index);
		}

		if (vscc.index == vscc.lowestLink) {
			Set<V> component = Read.from(stack).toSet();
			component.forEach(v_ -> sccs.get(v_).isStacked = false);
			components.add(IdentityKey.of(component));
		}
	}

}
