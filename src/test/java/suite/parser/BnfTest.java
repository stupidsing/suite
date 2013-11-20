package suite.parser;

import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

public class BnfTest {

	@Test
	public void testNumber() throws IOException {
		Bnf bnf = new Bnf(new FileReader("src/main/bnf/expression.bnf"));
		assertTrue(bnf.recursiveDescent("3456"));
	}

	@Test
	public void testExpr() throws IOException {
		Bnf bnf = new Bnf(new FileReader("src/main/bnf/expression.bnf"));
		assertTrue(bnf.recursiveDescent("12 + 34 + 56 + 78"));
	}

	// @Test
	public void testJava() throws IOException {
		Bnf bnf = new Bnf(new FileReader("src/main/bnf/java.bnf"));
		assertTrue(bnf.recursiveDescent("src/test/java/suite/parser/BnfTest.java"));
	}

}
