package suite.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.primitive.Bytes;
import suite.util.To;

public class RollingHashUtilTest {

	private RollingHashUtil rollingHashUtil = new RollingHashUtil();

	@Test
	public void test0() {
		byte b = 1, b1 = 1;

		var rollingHash = 0;
		rollingHash = rollingHashUtil.roll(rollingHash, b);
		rollingHash = rollingHashUtil.roll(rollingHash, b1);
		rollingHash = rollingHashUtil.unroll(rollingHash, b, 2);
		rollingHash = rollingHashUtil.unroll(rollingHash, b1, 1);

		assertEquals(0, rollingHash);
	}

	@Test
	public void test1() {
		var bytes = To.bytes("0123456789abcdef");
		var size = bytes.size();

		var rollingHash = rollingHashUtil.hash(bytes);

		for (var pos = 0; pos < size; pos++)
			rollingHash = rollingHashUtil.unroll(rollingHash, bytes.get(pos), size - pos);

		assertEquals(rollingHashUtil.hash(Bytes.empty), rollingHash);
	}

	@Test
	public void test2() {
		var bytes = To.bytes("0123456789abcdef");
		var size = bytes.size();

		var rollingHash = rollingHashUtil.hash(bytes.range(0, 10));

		for (var pos = 10; pos < size; pos++) {
			rollingHash = rollingHashUtil.unroll(rollingHash, bytes.get(pos - 10), 10);
			rollingHash = rollingHashUtil.roll(rollingHash, bytes.get(pos));
		}

		assertEquals(rollingHashUtil.hash(bytes.range(size - 10)), rollingHash);
	}

}
