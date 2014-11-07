package suite.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.TermOp;

public class FunctionalTemplatePreprocessorTest {

	@Test
	public void test() {
		List<Node> fruits = Arrays.<Node> asList(new Str("orange"), new Str("apple"), new Str("pear"));

		Map<String, Node> map = new HashMap<>();
		map.put("list", Tree.list(TermOp.OR____, fruits));
		map.put("title", new Str("My favourite things"));

		System.out.println(new FunctionalTemplatePreprocessor().render("" //
				+ "<html> \n" //
				+ "    <head> \n" //
				+ "        <#= title #> \n" //
				+ "    </head> \n" //
				+ "    </body> \n" //
				+ "        Fruits: \n" //
				+ "<# . (list | apply . map {fruit => id#>        <li><#= fruit #></li> \n" //
				+ "<#}) #>    <body> \n" //
				+ "</html> \n" //
		, map));
	}

}
