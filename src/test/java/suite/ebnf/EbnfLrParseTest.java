package suite.ebnf;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

public class EbnfLrParseTest {

	@Test
	public void testAnd() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\" \"1\"\n" //
				, "<digit>");

		assertNotNull(elp.parse("0 1"));
	}

	@Test
	public void testComplexity() {
		String e = "e0";
		StringBuilder sb = new StringBuilder(e + " ::= \"0\" | \"1\"\n");
		for (int i = 0; i < 6; i++) {
			String enext = "e" + (i + 1);
			String op = "op" + i;
			sb.append(enext + " ::= " + e + " | " + e + " \"" + op + "\" " + enext + "\n");
			e = enext;
		}

		System.out.println(sb.toString());
		EbnfLrParse elp = EbnfLrParse.of(sb.toString(), e);
		assertNotNull(elp.parse("0 op1 1"));
	}

	@Test
	public void testEntity() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				+ "<digit2> ::= <digit> <digit>\n" //
				, "<digit2>");

		assertNotNull(elp.parse("0 1"));
	}

	@Test
	public void testEof() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<nil> ::= ()\n" //
				, "<nil>");

		assertNotNull(elp.parse(""));
	}

	@Test
	public void testExpression0() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<expression> ::= <number> | <number> \"+\" <expression>\n" //
				+ "<number> ::= <digit> | <digit> <number>\n" //
				+ "<digit> ::= \"1\" | \"2\" | \"3\"\n" //
				, "<expression>");

		System.out.println(elp.parse("1 + 2 + 3"));
	}

	@Test
	public void testExpression1() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<e> ::= <e> \"*\" <b> | <e> \"+\" <b> | <b>\n" //
				+ "<b> ::= \"0\" | \"1\"\n" //
				, "<e>");

		System.out.println(elp.parse("0 * 0 + 1"));
	}

	@Test
	public void testList() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<list> ::= () | <list> \"0\"\n" //
				, "<list>");

		assertNotNull(elp.parse("0"));
	}

	@Test
	public void testOr() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				, "<digit>");

		assertNotNull(elp.parse("0"));
		assertNotNull(elp.parse("1"));
	}

	@Test
	public void testShiftReduceConflict() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<list> ::= () | \"0\" <list>\n" //
				, "<list>");

		assertNotNull(elp.parse("0 0 0 0 0"));
	}

	@Test
	public void testToken() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\"\n" //
				, "<digit>");

		assertNotNull(elp.parse("0"));
	}

}
