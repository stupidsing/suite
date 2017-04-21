package suite.sample;

import suite.util.Util;

public class ConwayGameOfLife {

	private static int size = 16;
	private boolean[][] game;

	public ConwayGameOfLife(String s) {
		this(new boolean[size][size]);
		int x = 0;
		for (String line : s.split("\n")) {
			int y = 0;
			for (char ch : Util.chars(line))
				game[x][y++] = !Character.isWhitespace(ch);
			x++;
		}
	}

	public ConwayGameOfLife(boolean[][] game) {
		this.game = game;
	}

	public int population() {
		int population = 0;
		for (int x = 1; x < size; x++)
			for (int y = 1; y < size; y++)
				population += game[x][y] ? 1 : 0;
		return population;

	}

	public ConwayGameOfLife evolve(ConwayGameOfLife cgol) {
		boolean[][] game0 = cgol.game;
		boolean[][] game1 = new boolean[size][size];

		for (int x = 1; x < size - 1; x++)
			for (int y = 1; y < size - 1; y++) {
				int sum = 0 //
						+ (game0[x - 1][y - 1] ? 1 : 0) //
						+ (game0[x - 1][y] ? 1 : 0) //
						+ (game0[x - 1][y + 1] ? 1 : 0) //
						+ (game0[x][y - 1] ? 1 : 0) //
						+ (game0[x][y + 1] ? 1 : 0) //
						+ (game0[x + 1][y - 1] ? 1 : 0) //
						+ (game0[x + 1][y] ? 1 : 0) //
						+ (game0[x + 1][y + 1] ? 1 : 0) //
				;

				game1[x][y] = sum == 3 || game0[x][y] && sum == 2;
			}

		return new ConwayGameOfLife(game1);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int y = 1; y < size; y++)
			sb.append((char) 65309);
		sb.append('\n');
		for (int x = 1; x < size; x++) {
			for (int y = 1; y < size; y++)
				sb.append((char) (game[x][y] ? 65327 : 12288));
			sb.append('\n');
		}
		return sb.toString();
	}

}
