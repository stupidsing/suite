package suite.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;
import suite.util.Util;

/**
 * Process #include tags.
 *
 * @author ywsing
 */
public class IncludePreprocessor implements Fun<String, String> {

	private static String open = "#include(";
	private static String close = ")";

	private File dir;

	public IncludePreprocessor(File dir) {
		this.dir = dir;
	}

	@Override
	public String apply(String in) {
		StringBuilder sb = new StringBuilder();
		try {
			doIncludes(dir, in, new HashSet<>(), sb);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return sb.toString();
	}

	private void doIncludes(File dir, String in, Set<String> included, StringBuilder sb) throws IOException {
		int start = 0;

		while (true) {
			int pos0 = ParseUtil.search(in, start, open);
			if (pos0 == -1)
				break;
			int pos1 = ParseUtil.search(in, pos0 + open.length(), close);
			if (pos1 == -1)
				break;

			sb.append(in.substring(start, pos0));
			File file = new File(dir, in.substring(pos0 + open.length(), pos1));

			if (included.add(file.getAbsolutePath()))
				doIncludes(file.getParentFile(), Util.read(file), included, sb);

			start = pos1 + close.length();
		}

		sb.append(in.substring(start));
	}

}
