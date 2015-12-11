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
	public void testOr() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\" | \"1\"\n");

		assertNotNull(elp.parse("<digit>", "0"));
		assertNotNull(elp.parse("<digit>", "1"));
	}

	@Test
	public void testEntity() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				+ "<digit2> ::= <digit> <digit>\n");

		assertNotNull(elp.parse("<digit2>", "0 1"));
	}

}
