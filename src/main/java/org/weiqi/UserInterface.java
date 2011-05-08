package org.weiqi;

import org.util.Util;
import org.weiqi.Weiqi.Occupation;

public class UserInterface {

	public static void display(Board board) {
		System.out.println(board.toString());
	}

	public static Board importBoard(String s) {
		Board board = new Board();
		String rows[] = s.split("\n");

		for (int x = 0; x < Weiqi.SIZE; x++) {
			String cols[] = rows[x].split(" ");

			for (int y = 0; y < Weiqi.SIZE; y++) {
				Occupation occupation = Occupation.EMPTY;

				for (Occupation o : Occupation.values())
					if (Util.equals(cols[y], o.display()))
						occupation = o;

				board.set(Coordinate.c(x, y), occupation);
			}
		}

		return board;
	}

}
