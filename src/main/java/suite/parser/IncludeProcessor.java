package suite.parser;

import java.io.File;
import java.io.IOException;

import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;
import suite.util.To;

/**
 * Process #include tags.
 * 
 * @author ywsing
 */
public class IncludeProcessor implements Fun<String, String> {

	public final static String open = "#include(";
	public final static String close = ")";

	private File dir;

	public IncludeProcessor(File dir) {
		this.dir = dir;
	}

	@Override
	public String apply(String in) {
		StringBuilder sb = new StringBuilder();
		try {
			doIncludes(dir, in, sb);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return sb.toString();
	}

	private void doIncludes(File dir, String in, StringBuilder sb) throws IOException {
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
			doIncludes(file.getParentFile(), To.string(file), sb);
			start = pos1;
		}

		sb.append(in.substring(start));
	}

}
