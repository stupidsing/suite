package suite.ebnf;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class EbnfLrParseTest {

	@Test
	public void testAnd() throws IOException {
		String grammar = "<digit> ::= \"0\" \"1\"\n";
		assertNotNull(new EbnfLrParse(EbnfGrammar.parse(new StringReader(grammar))).parse("<digit>", "0 1"));
	}

	@Test
	public void testOr() throws IOException {
		String grammar = "<digit> ::= \"0\" | \"1\"\n";

		assertNotNull(new EbnfLrParse(EbnfGrammar.parse(new StringReader(grammar))).parse("<digit>", "0"));
		assertNotNull(new EbnfLrParse(EbnfGrammar.parse(new StringReader(grammar))).parse("<digit>", "1"));
	}

	@Test
	public void testEntity() throws IOException {
		String grammar = "" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				+ "<digit2> ::= <digit> <digit>\n";

		assertNotNull(new EbnfLrParse(EbnfGrammar.parse(new StringReader(grammar))).parse("<digit2>", "0 1"));
	}

}
