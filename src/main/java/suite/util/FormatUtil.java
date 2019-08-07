package suite.util;

import static suite.util.Streamlet_.forInt;

import primal.Verbs.Build;
import suite.primitive.ReadInt;
import suite.streamlet.As;
import suite.streamlet.Read;

public class FormatUtil {

	public static String tablize(String s) {
		var arrays = Read.from(s.split("\n")).map(line -> line.split("\t")).collect();
		var nColumns = arrays.collect(ReadInt.lift(array -> array.length)).max();

		var rows = arrays.map(array -> To.array(nColumns, String.class, column -> column < array.length ? array[column] : ""));

		var widths = forInt(nColumns) //
				.collect(As.ints(column -> rows //
						.collect(ReadInt.lift(row -> row[column].length())).max())) //
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

}
