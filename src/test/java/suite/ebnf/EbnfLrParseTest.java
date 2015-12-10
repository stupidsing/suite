package suite.ebnf;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class EbnfLrParseTest {

	@Test
	public void testAnd() throws IOException {
		Ebnf ebnf = new Ebnf(new StringReader("" //
				+ "<digit> ::= \"0\" \"1\"\n" //
				+ ""), EbnfLrParse::new);
		assertNotNull(ebnf.parse("<digit>", "0 1"));
	}

	@Test
	public void testOr() throws IOException {
		Ebnf ebnf = new Ebnf(new StringReader("" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				+ ""), EbnfLrParse::new);
		assertNotNull(ebnf.parse("<digit>", "0"));
		assertNotNull(ebnf.parse("<digit>", "1"));
	}

	@Test
	public void testEntity() throws IOException {
		Ebnf ebnf = new Ebnf(new StringReader("" //
				+ "<digit> ::= \"0\" | \"1\"\n" //
				+ "<digit2> ::= <digit> <digit>\n" //
				+ ""), EbnfLrParse::new);
		assertNotNull(ebnf.parse("<digit2>", "0 1"));
	}

}
