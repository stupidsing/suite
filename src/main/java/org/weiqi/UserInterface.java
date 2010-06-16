package org.weiqi;

import org.weiqi.Weiqi.Board;

public class UserInterface {

	public static void display(Board board) {
		for (int x = 0; x < Weiqi.SIZE; x++) {
			for (int y = 0; y < Weiqi.SIZE; y++)
				System.out.print(board.position[x][y] + " ");
			System.out.println();
		}
	}

}
