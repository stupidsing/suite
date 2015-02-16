package suite.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import suite.text.Transform.Run;
import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;
import suite.util.Util;

/**
 * Remove comments.
 *
 * @author ywsing
 */
public class CommentTransformer implements Fun<String, List<Run>> {

	public static String openGroupComment = "-=";
	public static String closeGroupComment = "=-";
	public static String openLineComment = "--";
	public static String closeLineComment = "\n";

	private Set<Character> whitespaces;
	private String openComment;
	private String closeComment;

	public static CommentTransformer groupCommentTransformer(Set<Character> whitespaces) {
		return new CommentTransformer(whitespaces, openGroupComment, closeGroupComment);
	}

	public static CommentTransformer lineCommentTransformer(Set<Character> whitespaces) {
		return new CommentTransformer(whitespaces, openLineComment, closeLineComment);
	}

	private CommentTransformer(Set<Character> whitespaces, String openComment, String closeComment) {
		this.whitespaces = whitespaces;
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

	private boolean isWhitespaces(String in) {
		boolean result = true;
		for (char ch : Util.chars(in))
			result &= whitespaces.contains(ch);
		return result;
	}

}
