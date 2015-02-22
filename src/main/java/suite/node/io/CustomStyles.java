package suite.node.io;

import suite.Suite;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public class CustomStyles {

	public static Fun<Node, Node[]> braceMatcher = Suite.matcher(".0 {.1}");
	public static Fun<Node, Node[]> bracketMatcher = Suite.matcher("[.0]");

}
