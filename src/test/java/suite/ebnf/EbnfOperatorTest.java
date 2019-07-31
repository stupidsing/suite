package suite.ebnf;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import suite.ebnf.lr.LrParse;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.util.To;

public class EbnfOperatorTest {

	@Test
	public void testEbnf() throws IOException {
		var ebnf = new Ebnf(new StringReader(ebnf()));
		System.out.println(ebnf.parse("e0", "1 * 2 + 3"));
	}

	@Test
	public void testLr() throws IOException {
		var elp = LrParse.of(ebnf(), "e0");
		System.out.println(elp.parse("1 * 2 + 3"));
	}

	private String ebnf() {
		var s = To.string(sb -> {
			var i = 0;

			for (var operator : TermOp.values()) {
				var op = "\"" + operator.name_().trim() + "\"";
				var v = v(i++);
				var v1 = v(i);
				if (operator.assoc() == Assoc.LEFT)
					sb.append(v + " ::= " + v1 + " | " + v + " " + op + " " + v1 + "\n");
				else
					sb.append(v + " ::= " + v1 + " | " + v1 + " " + op + " " + v + "\n");
			}

			var vx = v(i);
			sb.append(vx + " ::= \"1\" | \"2\" | \"3\" | \"(\" " + v(0) + " \")\"\n");
		});

		System.out.println(s);

		return s;
	}

	private String v(int i) {
		return "e" + i;
	}

}
