package org.weiqi;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.util.Util;
import org.weiqi.Weiqi.Array;
import org.weiqi.Weiqi.Occupation;

import com.google.common.collect.Multimap;

public class Judge {

	public static Occupation checkByOccupationExistence(Board board) {
		Set<Occupation> players = new HashSet<Occupation>();
		for (Coordinate c : Coordinate.getAll())
			players.add(board.get(c));
		for (Occupation player : Weiqi.players)
			if (players.contains(player)
					&& !players.contains(player.opponent()))
				return player;
		return null;
	}

	public static void checkGroupsLiveness(Board board, Array<Boolean> alives) {
		GroupAnalysis ga = new GroupAnalysis(board);
		Map<Integer, Occupation> groupColors = ga.getGroupColors();
		Multimap<Integer, Integer> groupTouches = ga.getGroupTouches();

		// Judge which groups are eyes, i.e. surrounded by only one colour
		Map<Integer, Boolean> groupIsEye = Util.createHashMap();

		for (Entry<Integer, Occupation> entry : groupColors.entrySet())
			if (entry.getValue() == Occupation.EMPTY) {
				Integer groupId = entry.getKey();
				Set<Occupation> colors = new HashSet<Occupation>();

				for (Integer neighbourGroupId : groupTouches.get(groupId)) {
					Occupation color = groupColors.get(neighbourGroupId);
					if (color != Occupation.EMPTY)
						colors.add(color);
				}

				// Has two colours
				groupIsEye.put(groupId, colors.size() <= 1);
			}
	}

}
