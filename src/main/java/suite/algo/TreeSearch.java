package suite.algo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TreeSearch<State> {

	public interface Traverse<State> {
		public List<State> generate(State state);

		public boolean isDone(State state);
	}

	public static <State> State breadthFirst(Traverse<State> game, State state0) {
		Set<State> states0 = new HashSet<>();

		states0.add(state0);

		while (!states0.isEmpty()) {
			Set<State> states1 = new HashSet<>();

			for (State state : states0)
				if (!game.isDone(state))
					states1.addAll(game.generate(state));
				else
					return state;

			states0 = states1;
		}

		return null;
	}

	public static <State> State depthFirst(Traverse<State> game, State state0) {
		if (!game.isDone(state0)) {
			for (State state1 : game.generate(state0)) {
				State statex = depthFirst(game, state1);
				if (statex != null)
					return statex;
			}
			return null;
		} else
			return state0;
	}

}
