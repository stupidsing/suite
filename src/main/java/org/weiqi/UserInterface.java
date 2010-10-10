package org.weiqi;

public class UserInterface {

	public static void display(Board board) {
		for (int x = 0; x < Weiqi.SIZE; x++) {
			for (int y = 0; y < Weiqi.SIZE; y++)
				System.out.print(board.get(new Coordinate(x, y)) + " ");
			System.out.println();
		}
	}
}
