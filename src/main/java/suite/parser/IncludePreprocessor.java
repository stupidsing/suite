package suite.parser;

import static primal.statics.Rethrow.ex;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import primal.Verbs.ReadString;
import suite.text.Preprocess.Run;
import suite.util.ParseUtil;

/**
 * Process #include tags.
 *
 * @author ywsing
 */
public class IncludePreprocessor {

	private static String open = "#include(";
	private static String close = ")";

	private Set<Path> included = new HashSet<>();
	private Path dir;

	public IncludePreprocessor(Path dir) {
		this.dir = dir;
	}

	public List<Run> preprocess(String in) {
		return ex(() -> {
			var runs = new ArrayList<Run>();
			doIncludes(dir, in, true, runs);
			return runs;
		});
	}

	private void doIncludes(Path dir, String in, boolean isInput, List<Run> runs) {
		var start = 0;

		while (true) {
			var pos0 = ParseUtil.search(in, start, open);
			if (pos0 == -1)
				break;
			var pos1 = ParseUtil.search(in, pos0 + open.length(), close);
			if (pos1 == -1)
				break;

			if (isInput)
				runs.add(new Run(start, pos0));
			else
				runs.add(new Run(in.substring(start, pos0)));

			var path = dir.resolve(in.substring(pos0 + open.length(), pos1));

			if (included.add(path.toAbsolutePath()))
				doIncludes(path.getParent(), ReadString.from(path), false, runs);

			start = pos1 + close.length();
		}

		if (isInput)
			runs.add(new Run(start, in.length()));
		else
			runs.add(new Run(in.substring(start, in.length())));
	}

}
