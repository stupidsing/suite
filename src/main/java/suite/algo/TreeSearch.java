package suite.algo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TreeSearch<State> {

	public interface Traverse<State> {
		public List<State> generate(State state);

		public boolean isDone(State state);
	}

	public static <State> boolean breadthFirst(Traverse<State> game, State state) {
		Set<State> states0 = new HashSet<>();

		states0.add(state);

		while (!states0.isEmpty()) {
			Set<State> states1 = new HashSet<>();

			for (State state0 : states0)
				if (!game.isDone(state0))
					states1.addAll(game.generate(state0));
				else
					return true;

			states0 = states1;
		}

		return false;
	}

	public static <State> boolean depthFirst(Traverse<State> game, State state) {
		if (!game.isDone(state)) {
			for (State state1 : game.generate(state))
				if (depthFirst(game, state1))
					return true;
			return false;
		} else
			return true;
	}

}
