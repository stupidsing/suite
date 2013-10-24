package suite.fp.eval;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Test;

import suite.Suite;
import suite.fp.FunCompilerConfig;
import suite.node.Node;

public class FilterTest {

	@Test
	public void testDirect() {
		assertEquals("abcdef", eval("c => c", "abcdef"));
	}

	@Test
	public void testMap() {
		assertEquals("bcdefg", eval("map {`+ 1`}", "abcdef"));
	}

	@Test
	public void testSplit() {
		assertEquals("abc\ndef\nghi", eval("tail . concat . map {cons {10}} . split {32}", "abc def ghi"));
	}

	private static String eval(String program, String in) {
		try (Reader reader = new StringReader(in); Writer writer = new StringWriter()) {
			Node node = Suite.applyReader(reader, Suite.parse(program));
			FunCompilerConfig fcc = Suite.fcc(node, true);
			Suite.evaluateFunToWriter(fcc, writer);
			return writer.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
