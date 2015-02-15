package suite.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import suite.text.Transformer;
import suite.text.Transformer.Run;
import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;
import suite.util.Util;

/**
 * Remove comments.
 *
 * @author ywsing
 */
public class CommentPreprocessor implements Fun<String, String> {

	public static String openGroupComment = "-=";
	public static String closeGroupComment = "=-";
	public static String openLineComment = "--";
	public static String closeLineComment = "\n";

	private Set<Character> whitespaces;

	private class CommentTransformer implements Fun<String, List<Run>> {
		private String openComment;
		private String closeComment;

		private CommentTransformer(String openComment, String closeComment) {
			this.openComment = openComment;
			this.closeComment = closeComment;
		}

		@Override
		public List<Run> apply(String in) {
			int closeLength = !isWhitespaces(closeComment) ? closeComment.length() : 0;
			int start = 0;
			List<Run> runs = new ArrayList<>();

			while (true) {
				int pos0 = ParseUtil.search(in, start, openComment);
				if (pos0 == -1)
					break;
				int pos1 = in.indexOf(closeComment, pos0 + openComment.length());
				if (pos1 == -1)
					break;
				runs.add(new Run(start, pos0));
				start = pos1 + closeLength;
			}

			runs.add(new Run(start, in.length()));
			return runs;
		}
	}

	public CommentPreprocessor(Set<Character> whitespaces) {
		this.whitespaces = whitespaces;
	}

	@Override
	public String apply(String in) {
		in = Transformer.preprocessor(new CommentTransformer(openGroupComment, closeGroupComment)).apply(in);
		in = Transformer.preprocessor(new CommentTransformer(openLineComment, closeLineComment)).apply(in);
		return in;
	}

	private boolean isWhitespaces(String in) {
		boolean result = true;
		for (char ch : Util.chars(in))
			result &= whitespaces.contains(ch);
		return result;
	}

}
