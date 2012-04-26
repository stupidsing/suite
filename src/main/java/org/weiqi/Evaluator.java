package org.weiqi;

import java.util.Map.Entry;
import java.util.Set;

import org.util.Util;
import org.weiqi.Weiqi.Occupation;

public class Evaluator {

	private static final int PIECESCORE = 10;
	private static final int TERRITORYSCORE = 100;

	public static int evaluate(Occupation player, Board board) {
		int score = 0;
		Occupation opponent = player.opponent();

		// Count territories by counting groups
		GroupAnalysis ga = new GroupAnalysis(board);

		for (Entry<Integer, Occupation> entry : ga.getGroupColors().entrySet()) {
			Integer groupId = entry.getKey();
			Occupation color = ga.getColor(groupId);
			Set<Occupation> colors = Util.createHashSet();
			boolean us = false, theirs = false;

			// Count pieces
			if (color == player)
				score += PIECESCORE * ga.getCoords(groupId).size();
			else if (color == opponent)
				score -= PIECESCORE * ga.getCoords(groupId).size();

			// Count territory
			if (color == Occupation.EMPTY) {
				for (Integer neighbourGroupId : ga.getTouches(groupId))
					colors.add(ga.getColor(neighbourGroupId));

				us = colors.contains(player);
				theirs = colors.contains(opponent);
			} else if (color == player)
				us = true;
			else
				theirs = true;

			if (!us || !theirs) { // Do not count when it is nearby both colours
				int scoreDelta = TERRITORYSCORE * ga.getCoords(groupId).size();
				score += !theirs ? scoreDelta : -scoreDelta;
			}
		}

		return score;
	}

}
