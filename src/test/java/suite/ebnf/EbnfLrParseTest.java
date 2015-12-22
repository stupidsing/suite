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
	public void testEntity() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				+ "<digit2> ::= <digit> <digit>\n" //
				, "<digit2>");

		assertNotNull(elp.parse("0 1"));
	}

	@Test
	public void testExpression() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<expression> ::= <number> | <number> \"+\" <expression>\n" //
				+ "<number> ::= <digit> | <digit> <number>\n" //
				+ "<digit> ::= \"0\" | \"1\" | \"2\" | \"3\"\n" //
				, "<expression>");

		assertNotNull(elp.parse("1 + 2 + 3"));
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
	public void testList() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<list> ::= () | <list> <digit>\n" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				, "<list>");

		assertNotNull(elp.parse(""));
		assertNotNull(elp.parse("0"));
		assertNotNull(elp.parse("0 1 0 1"));
	}

}
