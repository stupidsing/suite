package suite.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.text.Transform.Run;
import suite.util.FileUtil;
import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;

/**
 * Process #include tags.
 *
 * @author ywsing
 */
public class IncludeTransformer implements Fun<String, List<Run>> {

	private static String open = "#include(";
	private static String close = ")";

	private Set<Path> included = new HashSet<>();
	private Path dir;

	public IncludeTransformer(Path dir) {
		this.dir = dir;
	}

	public List<Run> apply(String in) {
		List<Run> runs = new ArrayList<>();
		try {
			doIncludes(dir, in, true, runs);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return runs;
	}

	private void doIncludes(Path dir, String in, boolean isInput, List<Run> runs) throws IOException {
		int start = 0;

		while (true) {
			int pos0 = ParseUtil.search(in, start, open);
			if (pos0 == -1)
				break;
			int pos1 = ParseUtil.search(in, pos0 + open.length(), close);
			if (pos1 == -1)
				break;

			if (isInput)
				runs.add(new Run(start, pos0));
			else
				runs.add(new Run(in.substring(start, pos0)));

			Path path = dir.resolve(in.substring(pos0 + open.length(), pos1));

			if (included.add(path.toAbsolutePath()))
				doIncludes(path.getParent(), FileUtil.read(path), false, runs);

			start = pos1 + close.length();
		}

		if (isInput)
			runs.add(new Run(start, in.length()));
		else
			runs.add(new Run(in.substring(start, in.length())));
	}

}
