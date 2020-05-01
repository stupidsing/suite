package suite.text;

import org.junit.jupiter.api.Test;
import primal.primitive.adt.Bytes;
import suite.util.To;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RollingHashTest {

	private RollingHash rh = new RollingHash();

	@Test
	public void test0() {
		byte b = 1, b1 = 1;

		var rollingHash = 0;
		rollingHash = rh.roll(rollingHash, b);
		rollingHash = rh.roll(rollingHash, b1);
		rollingHash = rh.unroll(rollingHash, b, 2);
		rollingHash = rh.unroll(rollingHash, b1, 1);

		assertEquals(0, rollingHash);
	}

	@Test
	public void test1() {
		var bytes = To.bytes("0123456789abcdef");
		var size = bytes.size();

		var rollingHash = rh.hash(bytes);

		for (var pos = 0; pos < size; pos++)
			rollingHash = rh.unroll(rollingHash, bytes.get(pos), size - pos);

		assertEquals(rh.hash(Bytes.empty), rollingHash);
	}

	@Test
	public void test2() {
		var bytes = To.bytes("0123456789abcdef");
		var size = bytes.size();

		var rollingHash = rh.hash(bytes.range(0, 10));

		for (var pos = 10; pos < size; pos++) {
			rollingHash = rh.unroll(rollingHash, bytes.get(pos - 10), 10);
			rollingHash = rh.roll(rollingHash, bytes.get(pos));
		}

		assertEquals(rh.hash(bytes.range(size - 10)), rollingHash);
	}

}
