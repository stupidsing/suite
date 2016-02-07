package suite.weiqi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import suite.weiqi.GroupAnalysis.Group;
import suite.weiqi.Weiqi.Array;
import suite.weiqi.Weiqi.Occupation;

public class Judge {

	public static Occupation checkByOccupationExistence(Board board) {
		int nPiecesCount = 0;
		Set<Occupation> players = new HashSet<>();

		for (Coordinate c : Coordinate.all()) {
			Occupation color = board.get(c);
			players.add(color);
			nPiecesCount += color != Occupation.EMPTY ? 1 : 0;
		}

		if (1 < nPiecesCount)
			for (Occupation player : Weiqi.players)
				if (players.contains(player) && !players.contains(player.opponent()))
					return player;

		return null;
	}

	public static void checkGroupsLiveness(Board board, Array<Boolean> alives) {
		GroupAnalysis ga = new GroupAnalysis(board);

		// Judge which groups are eyes, i.e. surrounded by only one colour
		Map<Group, Boolean> groupIsEye = new HashMap<>();

		for (Group group : ga.getGroups())
			if (group.color == Occupation.EMPTY) {
				Set<Occupation> colors = new HashSet<>();

				for (Group neighborGroup : group.touches) {
					Occupation color = neighborGroup.color;
					if (color != Occupation.EMPTY)
						colors.add(color);
				}

				// Has two colours
				groupIsEye.put(group, colors.size() <= 1);
			}
	}

}
