package suite.node.io;

import java.util.List;

import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;

/**
 * Remove comments.
 * 
 * @author ywsing
 */
public class CommentProcessor implements Fun<String, String> {

	public final static String openGroupComment = "-=";
	public final static String closeGroupComment = "=-";
	public final static String openLineComment = "--";
	public final static String closeLineComment = "\n";

	private List<Character> whitespaces;

	public CommentProcessor(List<Character> whitespaces) {
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

		while (true) {
			int pos1 = ParseUtil.search(in, 0, open);
			if (pos1 == -1)
				return in;
			int pos2 = ParseUtil.search(in, pos1 + open.length(), close);
			if (pos2 == -1)
				return in;
			in = in.substring(0, pos1) + in.substring(pos2 + closeLength);
		}
	}

	private boolean isWhitespaces(String in) {
		boolean result = true;
		for (char ch : in.toCharArray())
			result &= whitespaces.contains(ch);
		return result;
	}

}
