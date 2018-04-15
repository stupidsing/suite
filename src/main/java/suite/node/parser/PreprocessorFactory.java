package suite.node.parser;

import java.util.List;
import java.util.Set;

import suite.node.io.Operator;
import suite.parser.CommentPreprocessor;
import suite.parser.IndentationPreprocessor;
import suite.parser.WhitespacePreprocessor;
import suite.text.Preprocess.Run;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class PreprocessorFactory {

	private static Set<Character> whitespaces = To.set('\t', '\r', '\n');

	public static List<Fun<String, List<Run>>> create(Operator[] operators) {
		Fun<String, List<Run>> gct = CommentPreprocessor.groupCommentPreprocessor(whitespaces)::preprocess;
		Fun<String, List<Run>> lct = CommentPreprocessor.lineCommentPreprocessor(whitespaces)::preprocess;
		Fun<String, List<Run>> it = new IndentationPreprocessor(operators)::preprocess;
		Fun<String, List<Run>> wt = new WhitespacePreprocessor(whitespaces)::preprocess;
		return List.of(gct, lct, it, wt);
	}

}
