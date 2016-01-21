package suite.os;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class ExecUtilTest {

	@Test
	public void test0() throws IOException {
		ExecUtil exec = new ExecUtil(new String[] { "git", "status", }, "");
		System.out.println(exec);

		assertEquals(0, exec.code);
		assertTrue(!exec.out.isEmpty());
		assertTrue(exec.err.isEmpty());
	}

	@Test
	public void test1() throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1024; i++)
			sb.append("01234567890123456789012345678901234567890123456789012345678901234567890123456789\n");
		String in = sb.toString();

		ExecUtil exec = new ExecUtil(new String[] { "cat", }, in);

		assertEquals(0, exec.code);
		assertEquals(in, exec.out);
		assertTrue(exec.err.isEmpty());
	}

}
