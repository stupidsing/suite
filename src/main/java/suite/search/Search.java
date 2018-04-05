package suite.search;

import java.util.HashSet;
import java.util.List;

public class Search<State> {

	public interface Traverser<State> {
		public List<State> generate(State state);

		public boolean evaluate(State state);
	}

	public static <State> State breadthFirst(Traverser<State> traverser, State state0) {
		var states0 = new HashSet<State>();

		states0.add(state0);

		while (!states0.isEmpty()) {
			var states1 = new HashSet<State>();

			for (var state : states0)
				if (!traverser.evaluate(state))
					states1.addAll(traverser.generate(state));
				else
					return state;

			states0 = states1;
		}

		return null;
	}

	public static <State> State depthFirst(Traverser<State> traverser, State state0) {
		if (!traverser.evaluate(state0)) {
			for (var state1 : traverser.generate(state0)) {
				var statex = depthFirst(traverser, state1);
				if (statex != null)
					return statex;
			}
			return null;
		} else
			return state0;
	}

}
