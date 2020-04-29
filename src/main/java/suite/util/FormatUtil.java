package suite.util;

import static suite.util.Streamlet_.forInt;

import primal.MoreVerbs.Read;
import primal.Verbs.Build;
import primal.primitive.IntMoreVerbs.LiftInt;
import suite.streamlet.As;

public class FormatUtil {

	public static String tablize(String s) {
		var arrays = Read.from(s.split("\n")).map(line -> line.split("\t")).collect();
		var nColumns = arrays.collect(LiftInt.of(array -> array.length)).max();

		var rows = arrays
				.map(array -> To.array(nColumns, String.class, column -> column < array.length ? array[column] : ""))
				.toArray(String[].class);

		var widths = forInt(nColumns)
				.collect(As.ints(column -> Read
						.from(rows)
						.collect(LiftInt.of(row -> row[column].length())).max()))
				.toArray();

		if (Boolean.TRUE)
			return build(nColumns, widths, rows);
		else
			return buildWithBorders(nColumns, widths, rows);
	}

	private static String build(int nColumns, int[] widths, String[][] rows) {
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

	private static String buildWithBorders(int nColumns, int[] widths, String[][] rows) {
		return Build.string(sb -> {
			var first = true;

			for (var row : rows) {
				if (first)
					appendBorder(sb, nColumns, widths, "┌", "┬", "─", "┐");
				else
					appendBorder(sb, nColumns, widths, "├", "┼", "─", "┤");

				first = false;

				for (var column = 0; column < nColumns; column++) {
					var cell = row[column];
					var width = widths[column];

					sb.append("│");
					sb.append(cell);

					for (var i = cell.length(); i < width; i++)
						sb.append(" ");
				}

				sb.append("│\n");
			}

			appendBorder(sb, nColumns, widths, "└", "┴", "─", "┘");
		});
	}

	private static void appendBorder(
			StringBuilder sb, int nColumns, int[] widths,
			String ch0, String ch1, String ch2, String ch3) {
		for (var column = 0; column < nColumns; column++) {
			var width = widths[column];
			sb.append(column == 0 ? ch0 : ch1);

			for (var i = 0; i < width; i++)
				sb.append(ch2);

		}

		sb.append(ch3 + "\n");
	}

}
