package suite.ebnf.lr;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class LrParseTest {

	@Test
	public void testAnd() throws IOException {
		var lrParse = LrParse.of("<digit> ::= \"0\" \"1\"\n", "<digit>");
		assertNotNull(lrParse.parse("0 1"));
	}

	@Test
	public void testComplexity() {
		var e = "e0";
		var sb = new StringBuilder(e + " ::= \"0\" | \"1\"\n");

		for (var i = 0; i < 6; i++) {
			var enext = "e" + (i + 1);
			var op = "op" + i;
			sb.append(enext + " ::= " + e + " | " + e + " \"" + op + "\" " + enext + "\n");
			e = enext;
		}

		var text = sb.toString();
		System.out.println(text);
		var lrParse = LrParse.of(text, e);
		assertNotNull(lrParse.parse("0 op1 1"));
	}

	@Test
	public void testEntity() throws IOException {
		var grammar = "" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				+ "<digit2> ::= <digit> <digit>\n";
		var lrParse = LrParse.of(grammar, "<digit2>");

		assertNotNull(lrParse.parse("0 1"));
	}

	@Test
	public void testEof() throws IOException {
		var lrParse = LrParse.of("<nil> ::= ()\n", "<nil>");
		assertNotNull(lrParse.parse(""));
	}

	@Test
	public void testExpression0() throws IOException {
		var grammar = "" //
				+ "<expression> ::= <number> | <number> \"+\" <expression>\n" //
				+ "<number> ::= <digit> | <digit> <number>\n" //
				+ "<digit> ::= \"1\" | \"2\" | \"3\"\n";
		var lrParse = LrParse.of(grammar, "<expression>");

		System.out.println(lrParse.parse("1 + 2 + 3"));
	}

	@Test
	public void testExpression1() throws IOException {
		var grammar = "" //
				+ "<e> ::= <e> \"*\" <b> | <e> \"+\" <b> | <b>\n" //
				+ "<b> ::= \"0\" | \"1\"\n";
		var lrParse = LrParse.of(grammar, "<e>");

		System.out.println(lrParse.parse("0 * 0 + 1"));
	}

	@Test
	public void testList() throws IOException {
		var lrParse = LrParse.of("<list> ::= () | <list> \"0\"\n", "<list>");
		assertNotNull(lrParse.parse("0"));
	}

	@Test
	public void testOr() throws IOException {
		var lrParse = LrParse.of("<digit> ::= \"0\" | \"1\"\n", "<digit>");
		assertNotNull(lrParse.parse("0"));
		assertNotNull(lrParse.parse("1"));
	}

	@Test
	public void testShiftReduceConflict() throws IOException {
		var lrParse = LrParse.of("<list> ::= () | \"0\" <list>\n", "<list>");
		assertNotNull(lrParse.parse("0 0 0 0 0"));
	}

	@Test
	public void testToken() throws IOException {
		var lrParse = LrParse.of("<digit> ::= \"0\"\n", "<digit>");
		assertNotNull(lrParse.parse("0"));
	}

}
