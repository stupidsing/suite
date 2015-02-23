package suite.node.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.node.io.Operator;
import suite.parser.CommentPreprocessor;
import suite.parser.IndentationPreprocessor;
import suite.parser.WhitespacePreprocessor;
import suite.text.Transform.Run;
import suite.util.FunUtil.Fun;

public class PreprocessorFactory {

	private static Set<Character> whitespaces = new HashSet<>(Arrays.asList('\t', '\r', '\n'));

	public static List<Fun<String, List<Run>>> create(Operator operators[]) {
		Fun<String, List<Run>> gct = CommentPreprocessor.groupCommentPreprocessor(whitespaces);
		Fun<String, List<Run>> lct = CommentPreprocessor.lineCommentPreprocessor(whitespaces);
		Fun<String, List<Run>> it = new IndentationPreprocessor(operators);
		Fun<String, List<Run>> wt = new WhitespacePreprocessor(whitespaces);
		return Arrays.asList(gct, lct, it, wt);
	}

}
