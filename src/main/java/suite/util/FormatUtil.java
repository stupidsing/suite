package suite.util;

import static suite.util.Streamlet_.forInt;

import primal.Verbs.Build;
import suite.primitive.IntVerbs.AsInt;
import suite.streamlet.As;
import suite.streamlet.Read;

public class FormatUtil {

	public static String tablize(String s) {
		var arrays = Read.from(s.split("\n")).map(line -> line.split("\t")).collect();
		var nColumns = arrays.collect(AsInt.lift(array -> array.length)).max();

		var rows = arrays.map(array -> To.array(nColumns, String.class, column -> column < array.length ? array[column] : ""));

		var widths = forInt(nColumns) //
				.collect(As.ints(column -> rows //
						.collect(AsInt.lift(row -> row[column].length())).max())) //
				.toArray();

		return Build.string(sb -> {
			for (var row : rows) {
				for (var column = 0; column < nColumns; column++) {
					var cell = row[column];
					var width = widths[column];

					sb.append(cell);

					for (var i = cell.length(); i < width; i++)
						sb.append(" ");
				}

				sb.append("\n");
			}
		});
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
