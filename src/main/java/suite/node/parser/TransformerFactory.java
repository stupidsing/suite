package suite.node.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.node.io.Operator;
import suite.parser.CommentTransformer;
import suite.parser.IndentationTransformer;
import suite.parser.WhitespaceTransformer;
import suite.text.Transform.Run;
import suite.util.FunUtil.Fun;

public class TransformerFactory {

	private static Set<Character> whitespaces = new HashSet<>(Arrays.asList('\t', '\r', '\n'));

	public static List<Fun<String, List<Run>>> create(Operator operators[]) {
		Fun<String, List<Run>> gct = CommentTransformer.groupCommentTransformer(whitespaces);
		Fun<String, List<Run>> lct = CommentTransformer.lineCommentTransformer(whitespaces);
		Fun<String, List<Run>> it = new IndentationTransformer(operators);
		Fun<String, List<Run>> wt = new WhitespaceTransformer(whitespaces);
		return Arrays.asList(gct, lct, it, wt);
	}

}
