package suite.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class AlphabetaTest {

	private static class TicTacToe {
		char player = 'O';
		char[] grid = new char[9];

		public String toString() {
			return "" //
					+ "\n" + c(grid[0]) + "|" + c(grid[1]) + "|" + c(grid[2]) //
					+ "\n" + c(grid[3]) + "|" + c(grid[4]) + "|" + c(grid[5]) //
					+ "\n" + c(grid[6]) + "|" + c(grid[7]) + "|" + c(grid[8]) //
					+ "\n";
		}

		private char c(char v) {
			return v != 0 ? v : ' ';
		}
	}

	private int[][] lines = new int[][] { //
			{ 0, 1, 2, }, //
			{ 3, 4, 5, }, //
			{ 6, 7, 8, }, //
			{ 0, 3, 6, }, //
			{ 1, 4, 7, }, //
			{ 2, 5, 8, }, //
			{ 0, 4, 8, }, //
			{ 2, 4, 6, }, //
	};

	private int[] scores = new int[] { 0, 1, 10, 10000, };

	private List<TicTacToe> generate(TicTacToe state) {
		List<TicTacToe> states = new ArrayList<>();

		if (!isEnd(state))
			for (int i = 0; i < 9; i++)
				if (state.grid[i] == 0) {
					TicTacToe state1 = new TicTacToe();
					state1.player = (char) ('O' + 'X' - state.player);
					state1.grid = Arrays.copyOf(state.grid, 9);
					state1.grid[i] = state.player;
					states.add(state1);
				}

		return states;
	}

	private boolean isEnd(TicTacToe state) {
		for (int[] line : lines) {
			char c0 = state.grid[line[0]];
			char c1 = state.grid[line[1]];
			char c2 = state.grid[line[2]];

			if (c0 != 0 && c0 == c1 && c1 == c2)
				return true;
		}

		return false;
	}

	private int evaluate(TicTacToe state) {
		int score = 0;
		for (int[] line : lines)
			score += evaluateLine(state, line);
		return score;
	}

	private int evaluateLine(TicTacToe state, int[] line) {
		int no = 0, nx = 0;

		for (int c = 0; c < 3; c++)
			if (state.grid[line[c]] == 'O')
				no++;
			else if (state.grid[line[c]] == 'X')
				nx++;

		int o = no == 0 || nx == 0 ? scores[no] - scores[nx] : 0;
		return state.player == 'O' ? o : -o;
	}

	@Test
	public void test() {
		Alphabeta<TicTacToe> ab = new Alphabeta<>(this::generate, this::evaluate);

		TicTacToe state = new TicTacToe();
		System.out.println(ab.search(state, 20));
	}

}
