package suite.parser;

import java.util.List;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;
import primal.adt.Pair;
import suite.node.Node;
import suite.node.Str;
import suite.node.io.BaseOp;
import suite.node.util.TreeUtil;

public class RenderFunctionalTemplateTest {

	@Test
	public void test() {
		var fruits = List.<Node>of(new Str("orange"), new Str("apple"), new Str("pear"));

		var map = Read //
				.from2(List.of( //
						Pair.of("list", TreeUtil.buildUp(BaseOp.OR____, fruits)), //
						Pair.of("title", new Str("My favourite things")))) //
				.toMap();

		System.out.println(new RenderFunctionalTemplate() //
				.render("""
						<html>
							<head>
								<#= title #>
							</head>
							</body>
								Fruits:
						<# . (list | apply . map_{fruit => id#>		<li><#= fruit #></li>
						<#}) #>	<body>
						</html>
						""", //
						map));
	}

}
