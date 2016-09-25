package suite.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import suite.adt.Pair;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.streamlet.As;
import suite.streamlet.Read;

public class FunctionalTemplateRendererTest {

	@Test
	public void test() {
		List<Node> fruits = Arrays.<Node> asList(new Str("orange"), new Str("apple"), new Str("pear"));

		Map<String, Node> map = Read.from2(Arrays.asList( //
				Pair.of("list", Tree.of(TermOp.OR____, fruits)) //
				, Pair.of("title", new Str("My favourite things")) //
				)) //
				.collect(As::map);

		System.out.println(new FunctionalTemplateRenderer().render("" //
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
