package org.ai;

import java.util.List;

public class AlphabetaSearch {

	public interface Game<State> {
		public List<State> generate(State state);

		public int evaluate(State state);
	}

	public static <State> int search(Game<State> game //
			, State state, int depth //
			, int alpha, int beta) {
		if (depth > 0) {
			List<State> states = game.generate(state);

			if (!states.isEmpty()) {
				for (State state1 : states) {
					int score = -search(game, state1, depth - 1, -beta, -alpha);

					if (score <= beta)
						beta = Math.max(score, beta);
					else
						return score;
				}

				return beta;
			}
		}

		return game.evaluate(state);
	}

}
