package org.weiqi;

import java.util.Set;

import org.util.Util;
import org.weiqi.GroupAnalysis.Group;
import org.weiqi.Weiqi.Occupation;

public class Evaluator {

	private static final int pieceScore = 10;
	private static final int territoryScore = 100;

	public static int evaluate(Occupation player, Board board) {
		int score = 0;
		Occupation opponent = player.opponent();

		// Count territories by counting groups
		GroupAnalysis ga = new GroupAnalysis(board);

		for (Group group : ga.getGroups()) {
			Occupation color = group.color;
			Set<Occupation> colors = Util.createHashSet();
			boolean us = false, theirs = false;

			// Count pieces
			if (color == player)
				score += pieceScore * group.coords.size();
			else if (color == opponent)
				score -= pieceScore * group.coords.size();

			// Count territory
			if (color == Occupation.EMPTY) {
				for (Group neighborGroup : group.touches)
					colors.add(neighborGroup.color);

				us = colors.contains(player);
				theirs = colors.contains(opponent);
			} else if (color == player)
				us = true;
			else
				theirs = true;

			if (!us || !theirs) { // Do not count when it is nearby both colours
				int scoreDelta = territoryScore * group.coords.size();
				score += !theirs ? scoreDelta : -scoreDelta;
			}
		}

		return score;
	}

}
