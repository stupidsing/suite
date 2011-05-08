package org.weiqi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.weiqi.Weiqi.Occupation;
import org.weiqi.uct.UctVisitor;

public class UctWeiqi {

	private final static Random random = new Random();

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
		public List<Coordinate> elaborateMoves() {
			GroupAnalysis ga = new GroupAnalysis(gameSet);

			List<Coordinate> moves = new ArrayList<Coordinate>( //
					Weiqi.SIZE * Weiqi.SIZE);

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

					if (hasBreath && gameSet.isMovePossible(c))
						moves.add(c);
				}

			return moves;
		}

		@Override
		public void playMove(Coordinate c) {
			gameSet.move(c);
		}

		@Override
		public boolean evaluateRandomOutcome() {
			Occupation me = gameSet.getNextPlayer();
			Coordinate move = null;

			// Move until someone cannot move anymore,
			// or maximum iterations reached
			for (int i = 0; i < 2 * Weiqi.AREA; i++) {

				// Try a random empty position, if that position does not work,
				// calls the heavier possible move method
				move = randomMove(findAllEmptyPositions());

				if (move == null || !gameSet.moveIfPossible(move)) {
					move = randomMove(elaborateMoves());
					if (move != null)
						gameSet.move(move);
				}

				if (move == null) // No moves can be played, current player lost
					break;
			}

			if (move == null)
				return gameSet.getNextPlayer() != me;
			else
				return Evaluator.evaluate(me, gameSet) > 0;
		}

		private Coordinate randomMove(List<Coordinate> moves) {
			int size = moves.size();
			return size > 0 ? moves.get(random.nextInt(size)) : null;
		}

		public List<Coordinate> findAllEmptyPositions() {
			List<Coordinate> moves = new ArrayList<Coordinate>( //
					Weiqi.SIZE * Weiqi.SIZE);
			for (Coordinate c : Coordinate.all())
				if (gameSet.get(c) == Occupation.EMPTY)
					moves.add(c);
			return moves;
		}
	}

}
