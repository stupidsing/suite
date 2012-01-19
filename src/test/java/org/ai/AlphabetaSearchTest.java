package org.ai;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ai.AlphabetaSearch.Game;
import org.junit.Test;

public class AlphabetaSearchTest {

	private static class TicTacToe {
		char player = 'O';
		char grid[] = new char[9];
	}

	@Test
	public void test() {
		assertTrue(AlphabetaSearch.search(new Game<TicTacToe>() {
			public List<TicTacToe> generate(TicTacToe state) {
				List<TicTacToe> states = new ArrayList<TicTacToe>();

				for (int i = 0; i < 9; i++)
					if (state.grid[i] == 0) {
						TicTacToe state1 = new TicTacToe();
						state1.player = (char) ('O' + 'X' - state.player);
						state1.grid = Arrays.copyOf(state1.grid, 9);
						state1.grid[i] = state.player;
						states.add(state1);
					}

				return states;
			}

			public int evaluate(TicTacToe state) {
				int score = 0;
				for (int i : new int[] { 0, 3, 6 })
					score += checkWin(state, i, 1);
				for (int i : new int[] { 0, 1, 2 })
					score += checkWin(state, i, 3);

				score += checkWin(state, 0, 4);
				score += checkWin(state, 2, 2);
				return score;
			}

			private int scores[] = new int[] { 0, 1, 10, 10000 };

			private int checkWin(TicTacToe state, int i, int j) {
				int no = 0, nx = 0;

				for (int c = 0; c < 3; c++) {
					if (state.grid[i] == 'O')
						no++;
					else if (state.grid[i] == 'X')
						nx++;

					i += j;
				}

				if (no == 0 || nx == 0)
					if (no > 0)
						return state.player == 'O' ? scores[no] : -scores[no];
					else
						return state.player == 'X' ? scores[nx] : -scores[nx];
				else
					return 0;
			}
		}, new TicTacToe(), 4, Integer.MIN_VALUE, Integer.MAX_VALUE) >= 10000);
	}

}
