package org.weiqi;

import org.weiqi.Weiqi.Array;
import org.weiqi.Weiqi.Occupation;

public class Evaluator {

	private final static int PIECESCORE = 10;
	private final static int TERRITORYSCORE = 100;

	public static int evaluate(Occupation side, Board board) {
		int score = 0;
		Occupation opponent = side.opponent();

		// Count pieces
		for (Coordinate c : Coordinate.getAll())
			if (board.get(c) == side)
				score += PIECESCORE;

		// Calculates absolute territory
		Array<Occupation> territory = new Array<Occupation>();
		for (Coordinate c : Coordinate.getAll()) {
			Occupation color = board.get(c);

			if (color != Occupation.EMPTY && territory.get(c) == null)
				for (Coordinate c1 : board.findGroup(c)) {
					for (Coordinate c2 : c1.getNeighbours())
						if (board.get(c2) == Occupation.EMPTY) {
							Occupation whose;
							if (territory.get(c2) != color.opponent())
								whose = color;
							else
								whose = Occupation.EMPTY; // Clashed

							for (Coordinate c3 : board.findGroup(c2))
								territory.set(c3, whose);
						}

					territory.set(c1, color);
				}
		}

		for (Coordinate c : Coordinate.getAll()) {
			Occupation color = territory.get(c);
			score += color == side ? TERRITORYSCORE : 0;
			score -= color == opponent ? TERRITORYSCORE : 0;
		}

		return score;
	}

}
