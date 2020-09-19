package suite.ebnf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import primal.Verbs.ReadString;
import suite.node.parser.FactorizeResult;

public class EbnfTest {

	@Test
	public void testExcept() throws IOException {
		var ebnf = new Ebnf(new StringReader("""
				non-alphas ::= (non-alpha)*
				non-alpha ::= <CHARACTER> /except/ ([a-z] | [A-Z])
				non-boolean ::= <IDENTIFIER> /except/ ("true" | "false")
				"""));
		assertNotNull(ebnf.check("non-alphas", "123!@#"));
		assertNotNull(ebnf.check("non-boolean", "beatles"));
		assertNull(ebnf.check("non-alphas", "456q$%^"));
		assertNull(ebnf.check("non-boolean", "false"));
	}

	@Test
	public void testCrudeScript() throws IOException {
		var ebnf = new Ebnf(new FileReader("src/main/ebnf/crude-script.ebnf"));
		System.out.println(ebnf.parse("crude-script", """
				{
					let f = p => p;
					return 1 + 2 * 3;
				}
				"""));
	}

	@Test
	public void testExpression() throws IOException {
		var ebnf = new Ebnf(new FileReader("src/main/ebnf/expression.ebnf"));
		System.out.println(ebnf.parse("<expression>", "1 + 2 + 3"));
	}

	@Test
	public void testHeadRecursion() throws IOException {
		var ebnf = new Ebnf(new StringReader("""
				number ::= number \"x\" digit | digit
				digit ::= [0-9]
				"""));
		System.out.println(ebnf.parse("number", "1"));
		System.out.println(ebnf.parse("number", "1x2"));
		System.out.println(ebnf.parse("number", "1x2x3x4"));
	}

	@Test
	public void testId() throws IOException {
		var ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("<IDENTIFIER>", "abc"));
	}

	@Test
	public void testJava() throws IOException {
		var ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		var s = ReadString.from("src/test/java/suite/fp/SieveTest.java");
		System.out.println(new Dump(ebnf.parse("CompilationUnit", s), s));
	}

	@Test
	public void testJavaExpression() throws IOException {
		var ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("Expression", "\"1\" + \"2\""));
	}

	@Test
	public void testJavaSimple() throws IOException {
		var ebnf = new Ebnf(new FileReader("src/main/ebnf/java.ebnf"));
		System.out.println(ebnf.parse("CompilationUnit", "public class C { public void f() { int a; } }"));
	}

	@Test
	public void testJson() throws IOException {
		var ebnf = new Ebnf(new FileReader("src/main/ebnf/json.ebnf"));
		System.out.println(ebnf.parse("json", "{ \"key\": [32, \"text\"] }"));
	}

	@Test
	public void testRefactor() throws IOException {
		var sql0 = "SELECT 0 FROM DUAL WHERE COL1 = 1 AND COL2 IN (SELECT 1 FROM DUAL) ORDER BY COL DESC";

		var ebnf = new Ebnf(new FileReader("src/main/ebnf/sql.ebnf"));

		var fr = rewrite( //
				ebnf, //
				"intersect-select", //
				"SELECT .0 FROM DUAL", //
				"SELECT .0 FROM DUAL WHERE COL2 = 1", //
				ebnf.parseFNode(sql0, "sql"));

		var sql1 = fr.unparse();

		assertEquals(sql1, "" //
				+ "SELECT 0 FROM DUAL WHERE COL1 = 1 AND COL2 IN (SELECT 1 FROM DUAL WHERE COL2 = 1) ORDER BY COL DESC");
	}

	@Test
	public void testSql() throws IOException {
		var sql = "SELECT 0 FROM DUAL WHERE COL1 = 1 AND COL2 IN (SELECT 1 FROM DUAL) ORDER BY COL DESC";
		var ebnf = new Ebnf(new FileReader("src/main/ebnf/sql.ebnf"));
		System.out.println(ebnf.parse("sql", sql));
	}

	private FactorizeResult rewrite(Ebnf ebnf, String entity, String from, String to, FactorizeResult fr0) {
		var frfrom = ebnf.parseFNode(from, entity);
		var frto = ebnf.parseFNode(to, entity);
		return FactorizeResult.rewrite(frfrom, frto, fr0);
	}

}
