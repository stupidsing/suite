package org.weiqi;

import org.weiqi.Weiqi.Occupation;

public class UserInterface {

	public static void display(Board board) {
		for (int x = 0; x < Weiqi.SIZE; x++) {
			for (int y = 0; y < Weiqi.SIZE; y++) {
				Coordinate c = new Coordinate(x, y);
				System.out.print(display(board.get(c)) + " ");
			}

			System.out.println();
		}
	}

	public static String display(Occupation color) {
		switch (color) {
		case BLACK:
			return "X";
		case WHITE:
			return "O";
		default:
			return " ";
		}
	}
}
