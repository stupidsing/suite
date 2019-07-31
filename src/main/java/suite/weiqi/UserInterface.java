package suite.weiqi;

import primal.String_;
import suite.weiqi.Weiqi.Occupation;

public class UserInterface {

	public static void display(GameSet gameSet) {
		display(gameSet.board);
	}

	public static void display(Board board) {
		System.out.println(board.toString());
	}

	public static Board importBoard(String s) {
		var board = new Board();
		var rows = s.split("\n");

		for (var x = 0; x < Weiqi.size; x++) {
			var cols = rows[x].split(" ");

			for (var y = 0; y < Weiqi.size; y++) {
				var occupation = Occupation.EMPTY;

				for (var o : Occupation.values())
					if (String_.equals(cols[y], o.display()))
						occupation = o;

				board.set(Coordinate.c(x, y), occupation);
			}
		}

		return board;
	}

}
