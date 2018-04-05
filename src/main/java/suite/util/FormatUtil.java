package suite.util;

import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.Ints_;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class FormatUtil {

	public static String tablize(String s) {
		Streamlet<String[]> arrays = Read.from(s.split("\n")).map(line -> line.split("\t")).collect(As::streamlet);
		var nColumns = arrays.collect(Obj_Int.lift(array -> array.length)).max();

		Streamlet<String[]> rows = arrays //
				.map(array -> To.array(nColumns, String.class, column -> column < array.length ? array[column] : ""));

		var widths = Ints_ //
				.range(nColumns) //
				.collect(Int_Int.lift(column -> rows //
						.collect(Obj_Int.lift(row -> row[column].length())).max())) //
				.toArray();

		var sb = new StringBuilder();

		for (String[] row : rows) {
			for (var column = 0; column < nColumns; column++) {
				var cell = row[column];
				var width = widths[column];

				sb.append(cell);

				for (var i = cell.length(); i < width; i++)
					sb.append(" ");
			}

			sb.append("\n");
		}

		return sb.toString();
	}

	public static String trimLeft(String s) {
		var length = s.length();
		var pos = 0;
		do
			if (!Character.isWhitespace(s.charAt(pos)))
				break;
		while (++pos < length);
		return s.substring(pos);
	}

	public static String trimRight(String s) {
		var pos = s.length();
		while (0 <= --pos)
			if (!Character.isWhitespace(s.charAt(pos)))
				break;
		return s.substring(0, pos + 1);
	}

}
