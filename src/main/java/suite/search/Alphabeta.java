package suite.search;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import suite.adt.Pair;
import suite.util.FunUtil.Fun;

public class Alphabeta<State> {

	private Fun<State, List<State>> generate;
	private Fun<State, Integer> evaluate;
	private Deque<State> moves = new ArrayDeque<>();

	public Alphabeta(Fun<State, List<State>> generate, Fun<State, Integer> evaluate) {
		this.generate = generate;
		this.evaluate = evaluate;
	}

	public List<State> search(State state, int depth) {
		moves.clear();
		return search0(state, depth, 1 + Integer.MIN_VALUE, Integer.MAX_VALUE).t1;
	}

	private Pair<Integer, List<State>> search0(State state, int depth, int alpha, int beta) {
		if (0 < depth) {
			List<State> states = generate.apply(state);

			if (!states.isEmpty()) {
				List<State> principalVariation = null;

				for (State state1 : states) {
					moves.push(state1);

					Pair<Integer, List<State>> result = search0(state1, depth - 1, -beta, -alpha);
					int score = -result.t0;

					if (alpha < score) {
						alpha = score;
						principalVariation = result.t1;
					}

					moves.pop();

					if (beta < score)
						break;
				}

				return Pair.of(alpha, principalVariation);
			}
		}

		List<State> moves1 = new ArrayList<>(moves);
		return Pair.of(evaluate.apply(state), moves1);
	}

}
