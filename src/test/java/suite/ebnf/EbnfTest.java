package suite.ebnf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

import suite.util.os.FileUtil;

public class EbnfTest {

	@Test
	public void testExcept() throws IOException {
		Ebnf ebnf = new Ebnf(new StringReader("" //
				+ "non-alphas ::= (non-alpha)* \n" //
				+ "non-alpha ::= <CHARACTER> /except/ ([a-z] | [A-Z]) \n" //
				+ "non-boolean ::= <IDENTIFIER> /except/ (\"true\" | \"false\") \n" //
		));
		assertNotNull(ebnf.check("123!@#", "non-alphas"));
		assertNotNull(ebnf.check("beatles", "non-boolean"));
		assertNull(ebnf.check("456q$%^", "non-alphas"));
		assertNull(ebnf.check("false", "non-boolean"));
	}

	@Test
	public void testExpression() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/expression.ebnf"));
		System.out.println(ebnf.parse("1 + 2 + 3", "<expression>"));
	}

	@Test
	public void testHeadRecursion() throws IOException {
		Ebnf ebnf = new Ebnf(new StringReader("" //
				+ "number ::= number \"x\" digit | digit \n" //
				+ "digit ::= [0-9] \n" //
		));
		assertNotNull(ebnf.check("1x2x3x4", "number"));
	}

	@Test
	public void testId() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("abc", "<IDENTIFIER>"));
	}

	@Test
	public void testJava() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		String s = FileUtil.read("src/test/java/suite/ebnf/EbnfTest.java");
		System.out.println(new EbnfDump(ebnf.parse(s), s));
	}

	@Test
	public void testJavaExpression() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("\"1\" + \"2\"", "Expression"));
	}

	@Test
	public void testJavaSimple() throws IOException {
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("public class C { public void f() { int a; } }"));
	}

	@Test
	public void testSql() throws IOException {
		String sql = "SELECT 0 FROM DUAL WHERE COL1 = 1 AND COL2 IN (SELECT 1 FROM DUAL) ORDER BY COL DESC";
		Ebnf ebnf = new Ebnf(new FileReader("src/main/ebnf/sql.ebnf"));
		System.out.println(ebnf.parse(sql, "sql"));
	}

}
