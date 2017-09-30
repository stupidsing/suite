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
		int nColumns = arrays.collect(Obj_Int.lift(array -> array.length)).max();

		Streamlet<String[]> rows = arrays //
				.map(array -> To.array(String.class, nColumns, column -> column < array.length ? array[column] : ""));

		int[] widths = Ints_ //
				.range(nColumns) //
				.collect(Int_Int.lift(column -> rows //
						.collect(Obj_Int.lift(row -> row[column].length())).max())) //
				.toArray();

		StringBuilder sb = new StringBuilder();

		for (String[] row : rows) {
			for (int column = 0; column < nColumns; column++) {
				String cell = row[column];
				int width = widths[column];

				sb.append(cell);

				for (int i = cell.length(); i < width; i++)
					sb.append(" ");
			}

			sb.append("\n");
		}

		return sb.toString();
	}

	public static String trimLeft(String s) {
		int pos = 0;
		do
			if (!Character.isWhitespace(s.charAt(pos)))
				break;
		while (++pos < s.length());
		return s.substring(pos);
	}

	public static String trimRight(String s) {
		int pos = s.length();
		while (0 <= --pos)
			if (!Character.isWhitespace(s.charAt(pos)))
				break;
		return s.substring(0, pos + 1);
	}

}
