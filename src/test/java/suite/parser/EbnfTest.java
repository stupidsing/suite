package suite.parser;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import suite.util.To;

public class EbnfTest {

	@Test
	public void testExpression() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/expression.ebnf"));
		System.out.println(ebnf.parse("1 + 2 + 3", 0, "<expression>"));
	}

	@Test
	public void testId() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("abc", 0, "<IDENTIFIER>"));
	}

	@Test
	public void testJavaExpression() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("\"1\" + \"2\"", 0, "Expression"));
	}

	@Test
	public void testJavaSimple() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("public class C { public void f() { int a; } }"));
	}

	@Test
	public void testJava() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse(To.string(new FileReader("src/test/java/suite/parser/EbnfTest.java"))));
	}

	@Test
	public void testSql() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/sql.ebnf"));
		System.out.println(ebnf.parse("SELECT 0 FROM DUAL WHERE COL1 = 1 AND COL2 = 2", 0, "sql"));
	}

}
