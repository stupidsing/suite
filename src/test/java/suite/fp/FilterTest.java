package suite.fp;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.io.Writer;

import org.junit.Test;

import suite.Suite;
import suite.os.FileUtil;
import suite.util.TempDir;
import suite.util.Util;

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

	// detects memory usage. Memory leak if there are more than 10000 instances
	// of Thunk, Frame, Tree or Node exists.
	@Test
	public void testMemoryUsage() {
		int size = 524288;

		Reader reader = new Reader() {
			private int count = size;

			public void close() {
			}

			public int read(char buffer[], int pos, int len) {
				int nBytesRead = Math.min(count, len);

				// makes sure there are new line characters, otherwise lines
				// function would blow off the stack
				if (0 < nBytesRead) {
					for (int i = 0; i < nBytesRead; i++)
						buffer[pos + i] = (char) ((count - i) % 64);
					count -= nBytesRead;
					return nBytesRead;
				} else
					return -1;
			}
		};

		Writer writer = new Writer() {
			private int count = 0;

			public void write(char buffer[], int pos, int len) {
				count += len;
				if (count == size) {
					Suite.proveLogic("find.all.memoized.clear, intern.map.clear");
					System.gc();
					System.gc();
					System.gc();
					System.out.println("Dump heap to check memory now");
					System.out.println("" //
							+ "jmap -histo " + FileUtil.getPid() //
							+ " | tee " + TempDir.resolve("jmap") //
							+ " | less");
					Util.sleepQuietly(10 * 1000l);
				}
			}

			public void flush() {
			}

			public void close() {
			}
		};

		Suite.evaluateFilterFun("id", reader, writer, true, false);
	}

	private static String eval(String program, String in) {
		return Suite.evaluateFilterFun(program, in, true, false);
	}

}
