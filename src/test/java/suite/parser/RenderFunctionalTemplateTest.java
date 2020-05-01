package suite.parser;

import org.junit.jupiter.api.Test;
import primal.MoreVerbs.Read;
import primal.adt.Pair;
import suite.node.Node;
import suite.node.Str;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;

import java.util.List;

public class RenderFunctionalTemplateTest {

	@Test
	public void test() {
		var fruits = List.<Node> of(new Str("orange"), new Str("apple"), new Str("pear"));

		var map = Read //
				.from2(List.of( //
						Pair.of("list", TreeUtil.buildUp(TermOp.OR____, fruits)), //
						Pair.of("title", new Str("My favourite things")))) //
				.toMap();

		System.out.println(new RenderFunctionalTemplate() //
				.render("" //
						+ "<html> \n" //
						+ "    <head> \n" //
						+ "        <#= title #> \n" //
						+ "    </head> \n" //
						+ "    </body> \n" //
						+ "        Fruits: \n" //
						+ "<# . (list | apply . map_{fruit => id#>        <li><#= fruit #></li> \n" //
						+ "<#}) #>    <body> \n" //
						+ "</html> \n", //
						map));
	}

}
