package suite.node.parser;

import primal.fp.Funs.Fun;
import primal.parser.Operator;
import suite.parser.CommentPreprocessor;
import suite.parser.IndentationPreprocessor;
import suite.parser.WhitespacePreprocessor;
import suite.text.Preprocess.Run;

import java.util.List;
import java.util.Set;

public class PreprocessorFactory {

	private static Set<Character> whitespaces = Set.of('\t', '\r', '\n');

	public static List<Fun<String, List<Run>>> create(Operator[] operators) {
		Fun<String, List<Run>> gct = CommentPreprocessor.ofGroupComment(whitespaces)::preprocess;
		Fun<String, List<Run>> lct = CommentPreprocessor.ofLineComment(whitespaces)::preprocess;
		Fun<String, List<Run>> it = new IndentationPreprocessor(operators)::preprocess;
		Fun<String, List<Run>> wt = new WhitespacePreprocessor(whitespaces)::preprocess;
		return List.of(gct, lct, it, wt);
	}

}
