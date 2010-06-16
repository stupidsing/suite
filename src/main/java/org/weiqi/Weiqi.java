package org.weiqi;

public class Weiqi {

	public final static int SIZE = 19;

	public enum Occupation {
		EMPTY, BLACK, WHITE
	};

	public static class Board {
		Occupation position[][] = new Occupation[SIZE][SIZE];

		public Board() {
			for (Coordinate c : Coordinate.getAll())
				set(c, Occupation.EMPTY);
		}

		public void move(Coordinate c, Occupation player) {
			Occupation current = get(c);
			if (current == Occupation.EMPTY) {
				set(c, player);
				killIfDead(c);
				for (Coordinate neighbour : c.getNeighbours())
					killIfDead(neighbour);
			} else
				throw new RuntimeException("Cannot move on occupied position");
		}

		private void killIfDead(Coordinate c) {
			if (c.isWithinBoard() && !hasBreath(c))
				kill(c);
		}

		private void kill(Coordinate c) {
			kill(c, get(c));
		}

		private void kill(Coordinate c, Occupation player) {
			if (c.isWithinBoard() && get(c) == player) {
				set(c, Occupation.EMPTY);
				for (Coordinate neighbour : c.getNeighbours())
					kill(neighbour, player);
			}
		}

		private boolean hasBreath(Coordinate c) {
			return hasBreath(c, get(c));
		}

		private boolean hasBreath(Coordinate c, Occupation player) {
			if (c.isWithinBoard()) {
				Occupation current = get(c);

				if (current == Occupation.EMPTY)
					return true;
				else if (current == player) {
					set(c, null); // Do not re-count
					boolean hasBreath = false;
					for (Coordinate neighbour : c.getNeighbours())
						if (hasBreath(neighbour, player)) {
							hasBreath = true;
							break;
						}
					set(c, current); // Set it back
					return hasBreath;
				} else
					return false;
			} else
				return false;
		}

		public void set(Coordinate c, Occupation color) {
			position[c.x][c.y] = color;
		}

		public Occupation get(Coordinate c) {
			return position[c.x][c.y];
		}
	}

}
