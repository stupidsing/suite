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

	public CommentPreprocessor(Set<Character> whitespaces) {
		this.whitespaces = whitespaces;
	}

	@Override
	public String apply(String in) {
		in = removeComments(in, openGroupComment, closeGroupComment);
		in = removeComments(in, openLineComment, closeLineComment);
		return in;
	}

	private String removeComments(String in, String open, String close) {
		int closeLength = !isWhitespaces(close) ? close.length() : 0;
		int start = 0;
		List<Run> runs = new ArrayList<>();

		while (true) {
			int pos0 = ParseUtil.search(in, start, open);
			if (pos0 == -1)
				break;
			int pos1 = in.indexOf(close, pos0 + open.length());
			if (pos1 == -1)
				break;
			runs.add(new Run(start, pos0));
			start = pos1 + closeLength;
		}

		runs.add(new Run(start, in.length()));
		return new Transformer().combineRuns(in, runs);
	}

	private boolean isWhitespaces(String in) {
		boolean result = true;
		for (char ch : Util.chars(in))
			result &= whitespaces.contains(ch);
		return result;
	}

}
