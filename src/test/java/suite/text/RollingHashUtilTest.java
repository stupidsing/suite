package suite.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.net.Bytes;
import suite.util.To;

public class RollingHashUtilTest {

	private RollingHashUtil rollingHashUtil = new RollingHashUtil();

	@Test
	public void test0() {
		byte b = 1, b1 = 1;

		int rollingHash = 0;
		rollingHash = rollingHashUtil.roll(rollingHash, b);
		rollingHash = rollingHashUtil.roll(rollingHash, b1);
		rollingHash = rollingHashUtil.unroll(rollingHash, b, 2);
		rollingHash = rollingHashUtil.unroll(rollingHash, b1, 1);

		assertEquals(0, rollingHash);
	}

	@Test
	public void test1() {
		Bytes bytes = To.bytes("0123456789abcdef");
		int size = bytes.size();

		int rollingHash = rollingHashUtil.hash(bytes);

		for (int pos = 0; pos < size; pos++)
			rollingHash = rollingHashUtil.unroll(rollingHash, bytes.byteAt(pos), size - pos);

		assertEquals(rollingHashUtil.hash(Bytes.emptyBytes), rollingHash);
	}

	@Test
	public void test2() {
		Bytes bytes = To.bytes("0123456789abcdef");
		int size = bytes.size();

		int rollingHash = rollingHashUtil.hash(bytes.subbytes(0, 10));

		for (int pos = 10; pos < size; pos++) {
			rollingHash = rollingHashUtil.unroll(rollingHash, bytes.byteAt(pos - 10), 10);
			rollingHash = rollingHashUtil.roll(rollingHash, bytes.byteAt(pos));
		}

		assertEquals(rollingHashUtil.hash(bytes.subbytes(size - 10)), rollingHash);
	}

}
