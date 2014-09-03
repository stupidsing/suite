package suite.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import suite.util.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;

/**
 * Process #include tags.
 *
 * @author ywsing
 */
public class IncludePreprocessor implements Fun<String, String> {

	private static String open = "#include(";
	private static String close = ")";

	private Path dir;

	public IncludePreprocessor(Path dir) {
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

	private void doIncludes(Path dir, String in, Set<Path> included, StringBuilder sb) throws IOException {
		int start = 0;

		while (true) {
			int pos0 = ParseUtil.search(in, start, open);
			if (pos0 == -1)
				break;
			int pos1 = ParseUtil.search(in, pos0 + open.length(), close);
			if (pos1 == -1)
				break;

			sb.append(in.substring(start, pos0));
			Path path = dir.resolve(in.substring(pos0 + open.length(), pos1));

			if (included.add(path.toAbsolutePath()))
				doIncludes(path.getParent(), FileUtil.read(path), included, sb);

			start = pos1 + close.length();
		}

		sb.append(in.substring(start));
	}

}
