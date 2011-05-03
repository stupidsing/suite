package org.weiqi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.weiqi.Weiqi.Occupation;

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
			Occupation winner = null;
			Occupation player = nextPlayer;

			// Move until someone cannot move anymore
			while (winner == null) {
				List<Coordinate> moves = findAllMoves();

				if (!moves.isEmpty()) {
					Coordinate c = moves.get(random.nextInt(moves.size()));
					board.move(c, nextPlayer);
					nextPlayer = nextPlayer.opponent();
				} else
					winner = nextPlayer.opponent();
			}

			return player == winner;
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
