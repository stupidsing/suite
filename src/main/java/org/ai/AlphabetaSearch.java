package org.ai;

import java.util.ArrayList;
import java.util.List;

import org.util.Util.Pair;

public class AlphabetaSearch<State> {

	public interface Game<State> {
		public List<State> generate(State state);

		public int evaluate(State state);
	}

	private Game<State> game;
	private List<State> moves = new ArrayList<State>();

	public AlphabetaSearch(Game<State> game) {
		this.game = game;
	}

	public List<State> search(State state, int depth) {
		moves.clear();
		return search0(state, depth, 1 + Integer.MIN_VALUE, Integer.MAX_VALUE).t2;
	}

	public Pair<Integer, List<State>> search0(State state, int depth //
			, int alpha, int beta) {
		if (depth > 0) {
			List<State> states = game.generate(state);

			if (!states.isEmpty()) {
				List<State> principalVariation = null;

				for (State state1 : states) {
					moves.add(state1);

					Pair<Integer, List<State>> result = search0(state1,
							depth - 1, -beta, -alpha);
					int score = -result.t1;

					if (score > alpha) {
						alpha = score;
						principalVariation = result.t2;
					}

					moves.remove(moves.size() - 1);

					if (score > beta)
						break;
				}

				return Pair.create(alpha, principalVariation);
			}
		}

		List<State> moves1 = new ArrayList<State>(moves);
		return Pair.create(game.evaluate(state), moves1);
	}

}
