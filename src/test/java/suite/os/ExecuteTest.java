package suite.os;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExecuteTest {

	@Test
	public void test0() {
		Execute exec = new Execute(new String[] { "git", "status", });
		System.out.println(exec);

		assertEquals(0, exec.code);
		assertTrue(!exec.out.isEmpty());
		assertTrue(exec.err.isEmpty());
	}

	@Test
	public void test1() {
		var sb = new StringBuilder();
		for (var i = 0; i < 1024; i++)
			sb.append("01234567890123456789012345678901234567890123456789012345678901234567890123456789\n");
		var in = sb.toString();

		Execute exec = new Execute(new String[] { "cat", }, in);

		assertEquals(0, exec.code);
		assertEquals(in, exec.out);
		assertTrue(exec.err.isEmpty());
	}

}
