package org.weiqi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.weiqi.Weiqi.Occupation;
import org.weiqi.uct.UctVisitor;

public class UctWeiqi {

	private final static Random random = new Random();

	public static class Visitor implements UctVisitor<Coordinate> {
		private Board board;
		private Occupation nextPlayer;

		public Visitor(Board board, Occupation nextPlayer) {
			this.board = board;
			this.nextPlayer = nextPlayer;
		}

		@Override
		public UctVisitor<Coordinate> cloneVisitor() {
			return new Visitor(new Board(board), nextPlayer);
		}

		@Override
		public Iterable<Coordinate> elaborateMoves() {
			return findAllMoves();
		}

		@Override
		public void playMove(Coordinate c) {
			board.move(c, nextPlayer);
			nextPlayer = nextPlayer.opponent();
		}

		@Override
		public boolean evaluateRandomOutcome() {
			Occupation me = nextPlayer;
			Coordinate move = null;

			// Move until someone cannot move anymore,
			// or maximum iterations reached
			for (int i = 0; i < 2 * Weiqi.AREA; i++) {

				// Try a random empty position, if that position does not work,
				// calls the heavier possible move method
				move = randomMove(findAllEmptyPositions());

				if (move == null || !board.moveIfPossible(move, nextPlayer)) {
					move = randomMove(findAllMoves());
					if (move != null)
						board.move(move, nextPlayer);
				}

				if (move == null) // No moves can be played, current player lost
					break;

				nextPlayer = nextPlayer.opponent();
			}

			if (move == null)
				return nextPlayer != me;
			else
				return Evaluator.evaluate(me, board) > 0;
		}

		private Coordinate randomMove(List<Coordinate> moves) {
			int size = moves.size();
			return size > 0 ? moves.get(random.nextInt(size)) : null;
		}

		public List<Coordinate> findAllEmptyPositions() {
			List<Coordinate> moves = new ArrayList<Coordinate>( //
					Weiqi.SIZE * Weiqi.SIZE);
			for (Coordinate c : Coordinate.all())
				if (board.get(c) == Occupation.EMPTY)
					moves.add(c);
			return moves;
		}

		public List<Coordinate> findAllMoves() {
			GroupAnalysis ga = new GroupAnalysis(board);

			List<Coordinate> moves = new ArrayList<Coordinate>( //
					Weiqi.SIZE * Weiqi.SIZE);

			for (Coordinate c : Coordinate.all())
				if (board.get(c) == Occupation.EMPTY) {
					Integer groupId = ga.getGroupId(c);
					boolean hasBreath;

					if (ga.getCoords(groupId).size() == 1) { // A tight space
						hasBreath = false;

						for (Integer groupId1 : ga.getTouches(groupId)) {
							int nBreathes = ga.getNumberOfBreathes(groupId1);

							if (ga.getColor(groupId1) == nextPlayer)
								hasBreath |= nBreathes > 1;
							else
								hasBreath |= nBreathes <= 1;
						}
					} else
						hasBreath = true;

					if (hasBreath)
						moves.add(c);
				}

			return moves;
		}
	}

}
