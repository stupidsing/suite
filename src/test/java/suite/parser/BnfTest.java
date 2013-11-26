package suite.parser;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import suite.util.To;

public class BnfTest {

	@Test
	public void testNumber() throws IOException {
		Bnf bnf = new Bnf(new FileReader("src/main/bnf/expression.bnf"));
		System.out.println(bnf.parse("3456"));
	}

	@Test
	public void testExpr() throws IOException {
		Bnf bnf = new Bnf(new FileReader("src/main/bnf/expression.bnf"));
		System.out.println(bnf.parse("12 + 34 + 56 + 78"));
	}

	@Test
	public void testJava0() throws IOException {
		Bnf bnf = new Bnf(new FileReader("src/main/bnf/java.bnf"));
		System.out.println(bnf.parse("public class A { int a; }"));
	}

	@Test
	public void testJava() throws IOException {
		Bnf bnf = new Bnf(new FileReader("src/main/bnf/java.bnf"));
		System.out.println(bnf.parse(To.string(new FileReader("src/test/java/suite/parser/BnfTest.java"))));
	}

}
