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
	public void testEof() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<nil> ::= ()\n" //
				, "<nil>");

		assertNotNull(elp.parse(""));
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

		assertNotNull(elp.parse("0"));
	}

	@Test
	public void testToken() throws IOException {
		EbnfLrParse elp = EbnfLrParse.of("" //
				+ "<digit> ::= \"0\"\n" //
				, "<digit>");

		assertNotNull(elp.parse("0"));
	}

}
