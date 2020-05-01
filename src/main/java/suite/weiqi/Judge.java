package suite.weiqi;

import suite.weiqi.GroupAnalysis.Group;
import suite.weiqi.Weiqi.Array;
import suite.weiqi.Weiqi.Occupation;

import java.util.HashMap;
import java.util.HashSet;

public class Judge {

	public static Occupation checkByOccupationExistence(Board board) {
		var nPiecesCount = 0;
		var players = new HashSet<>();

		for (var c : Coordinate.all()) {
			var color = board.get(c);
			players.add(color);
			nPiecesCount += color != Occupation.EMPTY ? 1 : 0;
		}

		if (1 < nPiecesCount)
			for (var player : Weiqi.players)
				if (players.contains(player) && !players.contains(player.opponent()))
					return player;

		return null;
	}

	public static void checkGroupsLiveness(Board board, Array<Boolean> alives) {
		var ga = new GroupAnalysis(board);

		// judge which groups are eyes, i.e. surrounded by only one colour
		var groupIsEye = new HashMap<Group, Boolean>();

		for (var group : ga.getGroups())
			if (group.color == Occupation.EMPTY) {
				var colors = new HashSet<>();

				for (var neighborGroup : group.touches) {
					var color = neighborGroup.color;
					if (color != Occupation.EMPTY)
						colors.add(color);
				}

				// has two colours
				groupIsEye.put(group, colors.size() <= 1);
			}
	}

}
