package suite.ebnf;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

public class EbnfLrParseTest {

	@Test
	public void testAnd() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\" \"1\"\n");

		assertNotNull(elp.parse("<digit>", "0 1"));
	}

	@Test
	public void testEntity() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				+ "<digit2> ::= <digit> <digit>\n");

		assertNotNull(elp.parse("<digit2>", "0 1"));
	}

	@Test
	public void testExpression() throws IOException {
		String grammar = "" //
				+ "<expression> ::= <number> | <number> \"+\" <expression>\n" //
				+ "<number> ::= <digit> | <digit> <number>\n" //
				+ "<digit> ::= \"0\" | \"1\" | \"2\" | \"3\"\n";

		assertNotNull(EbnfLrParse.of(grammar).parse("<expression>", "1 + 2 + 3"));
	}

	@Test
	public void testOr() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\" | \"1\"\n");

		assertNotNull(elp.parse("<digit>", "0"));
		assertNotNull(elp.parse("<digit>", "1"));
	}

	@Test
	public void testList() throws IOException {
		String grammar = "" //
				+ "<list> ::= () | <list> <digit>\n" //
				+ "<digit> ::= \"0\" | \"1\"\n";

		assertNotNull(EbnfLrParse.of(grammar).parse("<list>", ""));
		assertNotNull(EbnfLrParse.of(grammar).parse("<list>", "0"));
		assertNotNull(EbnfLrParse.of(grammar).parse("<list>", "0 1 0 1"));
	}

}
