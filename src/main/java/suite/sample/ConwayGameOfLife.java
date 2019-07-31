package suite.sample;

import suite.streamlet.Read;
import suite.util.To;

public class ConwayGameOfLife {

	private static int size = 16;
	private boolean[][] game;

	public ConwayGameOfLife(String s) {
		this(new boolean[size][size]);
		var x = 0;
		for (var line : s.split("\n")) {
			var y = 0;
			for (var ch : Read.chars(line))
				game[x][y++] = !Character.isWhitespace(ch);
			x++;
		}
	}

	public ConwayGameOfLife(boolean[][] game) {
		this.game = game;
	}

	public int population() {
		var population = 0;
		for (var x = 1; x < size; x++)
			for (var y = 1; y < size; y++)
				population += game[x][y] ? 1 : 0;
		return population;

	}

	public ConwayGameOfLife evolve(ConwayGameOfLife cgol) {
		var game0 = cgol.game;
		var game1 = new boolean[size][size];

		for (var x = 1; x < size - 1; x++)
			for (var y = 1; y < size - 1; y++) {
				var sum = 0 //
						+ (game0[x - 1][y - 1] ? 1 : 0) //
						+ (game0[x - 1][y + 0] ? 1 : 0) //
						+ (game0[x - 1][y + 1] ? 1 : 0) //
						+ (game0[x + 0][y - 1] ? 1 : 0) //
						+ (game0[x + 0][y + 1] ? 1 : 0) //
						+ (game0[x + 1][y - 1] ? 1 : 0) //
						+ (game0[x + 1][y + 0] ? 1 : 0) //
						+ (game0[x + 1][y + 1] ? 1 : 0) //
				;

				game1[x][y] = sum == 3 || game0[x][y] && sum == 2;
			}

		return new ConwayGameOfLife(game1);
	}

	@Override
	public String toString() {
		return To.string(sb -> {
			for (var y = 1; y < size; y++)
				sb.append((char) 65309);
			sb.append('\n');
			for (var x = 1; x < size; x++) {
				for (var y = 1; y < size; y++)
					sb.append((char) (game[x][y] ? 65327 : 12288));
				sb.append('\n');
			}
		});
	}

}
