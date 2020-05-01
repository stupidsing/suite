package suite.weiqi;

import suite.weiqi.Weiqi.Occupation;

import java.util.HashSet;

public class Evaluator {

	private static int pieceScore = 10;
	private static int territoryScore = 100;

	public static int evaluate(Occupation player, Board board) {
		var score = 0;
		var opponent = player.opponent();

		// count territories by counting groups
		var ga = new GroupAnalysis(board);

		for (var group : ga.getGroups()) {
			var color = group.color;
			var colors = new HashSet<>();
			boolean us = false, theirs = false;

			// count pieces
			if (color == player)
				score += pieceScore * group.coords.size();
			else if (color == opponent)
				score -= pieceScore * group.coords.size();

			// count territory
			if (color == Occupation.EMPTY) {
				for (var neighborGroup : group.touches)
					colors.add(neighborGroup.color);

				us = colors.contains(player);
				theirs = colors.contains(opponent);
			} else if (color == player)
				us = true;
			else
				theirs = true;

			if (!us || !theirs) { // do not count when it is nearby both colours
				var scoreDelta = territoryScore * group.coords.size();
				score += !theirs ? scoreDelta : -scoreDelta;
			}
		}

		return score;
	}

}
