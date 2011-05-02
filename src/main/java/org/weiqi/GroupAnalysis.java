package org.weiqi;

import java.util.Collection;
import java.util.Map;

import org.util.Util;
import org.weiqi.Weiqi.Array;
import org.weiqi.Weiqi.Occupation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class GroupAnalysis {

	private Board board;
	private Array<Integer> groupIdArray = Array.create();
	private Map<Integer, Occupation> groupColors = Util.createHashMap();
	private Multimap<Integer, Coordinate> groupCoords = HashMultimap.create();
	private Multimap<Integer, Integer> groupTouches = HashMultimap.create();
	private Map<Integer, Integer> groupBreathes = Util.createHashMap();

	public GroupAnalysis(Board board) {
		this.board = board;
		assignGroups();
		assignGroupSurroundings();
	}

	private void assignGroups() {

		// Assign group numbers to groups, including empty groups
		int nGroups = 0;
		Map<Integer, Integer> parentGroupIds = Util.createHashMap();

		for (Coordinate c : Coordinate.getAll()) {
			Occupation color = board.get(c);
			Integer groupId = null;

			for (Coordinate c1 : c.leftOrUp())
				if (board.get(c1) == color) {
					Integer newGroupId = groupIdArray.get(c1);

					if (newGroupId != null) {
						while (parentGroupIds.containsKey(newGroupId))
							newGroupId = parentGroupIds.get(newGroupId);

						groupId = Math.min(groupId, newGroupId);
						newGroupId = Math.max(groupId, newGroupId);

						if (newGroupId.intValue() != groupId.intValue())
							parentGroupIds.put(newGroupId, groupId);
					}
				}

			groupIdArray.set(c, groupId != null ? groupId : ++nGroups);
		}

		// Reduces group ID to parent and creates coordinate-to-group-ID mapping
		for (Coordinate c : Coordinate.getAll()) {
			Integer groupId = parentGroupIds.get(groupIdArray.get(c));
			groupIdArray.set(c, groupId);
			groupColors.put(groupId, board.get(c));
			groupCoords.put(groupId, c);
		}
	}

	private void assignGroupSurroundings() {
		for (Integer groupId : groupColors.keySet())
			groupBreathes.put(groupId, 0);

		for (Coordinate c : Coordinate.getAll()) {
			Integer groupId = groupIdArray.get(c);

			for (Coordinate c1 : c.leftOrUp()) {
				Integer groupId1 = groupIdArray.get(c1);
				groupTouches.put(groupId, groupId1);
				groupTouches.put(groupId1, groupId);

				if (board.get(c) == Occupation.EMPTY)
					increaseGroupBreath(groupId1);
				if (board.get(c1) == Occupation.EMPTY)
					increaseGroupBreath(groupId);
			}
		}
	}

	private void increaseGroupBreath(Integer groupId) {
		Integer nBreathes = groupBreathes.get(groupId);
		nBreathes = nBreathes != null ? nBreathes : 0;
		groupBreathes.put(groupId, nBreathes + 1);
	}

	public Array<Integer> getGroupIdArray() {
		return groupIdArray;
	}

	public Map<Integer, Occupation> getGroupColors() {
		return groupColors;
	}

	public Collection<Coordinate> getCoords(int groupId) {
		return groupCoords.get(groupId);
	}

	public Collection<Integer> getTouches(int groupId) {
		return groupTouches.get(groupId);
	}

	public int getBreathes(int groupId) {
		return groupBreathes.get(groupId);
	}

}
