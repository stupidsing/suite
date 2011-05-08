package org.weiqi;

import org.weiqi.GameSet.MoveCommand;
import org.weiqi.Weiqi.Occupation;
import org.weiqi.uct.UctVisitor;

public class UctWeiqi {

	public static class Visitor implements UctVisitor<Coordinate> {
		private GameSet gameSet;

		public Visitor(GameSet gameSet) {
			this.gameSet = gameSet;
		}

		@Override
		public UctVisitor<Coordinate> cloneVisitor() {
			return new Visitor(new GameSet(gameSet));
		}

		@Override
		public RandomList<Coordinate> elaborateMoves() {
			GroupAnalysis ga = new GroupAnalysis(gameSet);

			RandomList<Coordinate> moves = new RandomList<Coordinate>();

			for (Coordinate c : Coordinate.all())
				if (gameSet.get(c) == Occupation.EMPTY) {
					Integer groupId = ga.getGroupId(c);
					boolean hasBreath;

					if (ga.getCoords(groupId).size() == 1) { // A tight space
						hasBreath = false;

						for (Integer groupId1 : ga.getTouches(groupId)) {
							int nBreathes = ga.getNumberOfBreathes(groupId1);
							Occupation nextPlayer = gameSet.getNextPlayer();

							if (ga.getColor(groupId1) == nextPlayer)
								hasBreath |= nBreathes > 1;
							else
								hasBreath |= nBreathes <= 1;
						}
					} else
						hasBreath = true;

					if (hasBreath && gameSet.isMovePossible(new MoveCommand(c)))
						moves.add(c);
				}

			return moves;
		}

		@Override
		public void playMove(Coordinate c) {
			gameSet.move(new MoveCommand(c));
		}

		@Override
		public boolean evaluateRandomOutcome() {
			Occupation me = gameSet.getNextPlayer();
			RandomList<Coordinate> empties = findAllEmptyPositions();
			Coordinate pos;
			MoveCommand move = null;

			// Move until someone cannot move anymore,
			// or maximum iterations reached
			for (int i = 0; i < 2 * Weiqi.AREA; i++) {
				move = null;

				// Try a random empty position, if that position does not work,
				// calls the heavier possible move method
				if ((pos = empties.remove()) != null
						&& !gameSet.moveIfPossible(move = new MoveCommand(pos)))
					move = null;

				if (move == null && (pos = elaborateMoves().remove()) != null)
					gameSet.move(move = new MoveCommand(pos));

				if (move != null) { // Add empty positions back to empty group
					int j = 0;

					for (Coordinate c1 : move.position.neighbours())
						if (move.neighbourColors[j++] != gameSet.get(c1))
							for (Coordinate c2 : gameSet.findGroup(c1))
								empties.add(c2);
				} else
					break; // No moves can be played, current player lost
			}

			if (move == null)
				return gameSet.getNextPlayer() != me;
			else
				return Evaluator.evaluate(me, gameSet) > 0;
		}

		public RandomList<Coordinate> findAllEmptyPositions() {
			RandomList<Coordinate> moves = new RandomList<Coordinate>();

			for (Coordinate c : Coordinate.all())
				if (gameSet.get(c) == Occupation.EMPTY)
					moves.add(c);

			return moves;
		}
	}

}
