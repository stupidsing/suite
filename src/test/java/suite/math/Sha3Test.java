package suite.math;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class Sha3Test {

	@Test
	public void test() {
		var longString = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

		var empty = new byte[0];
		var bs = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.US_ASCII);
		var lbs = longString.getBytes(StandardCharsets.US_ASCII);

		assertEquals(0x6B, Byte.toUnsignedInt(sha3(224, empty)[0]));
		assertEquals(0xA7, Byte.toUnsignedInt(sha3(256, empty)[0]));
		assertEquals(0x0C, Byte.toUnsignedInt(sha3(384, empty)[0]));
		assertEquals(0xA6, Byte.toUnsignedInt(sha3(512, empty)[0]));
		assertEquals(0xA8, Byte.toUnsignedInt(sha3(224, (byte) 0x30)[0]));
		assertEquals(0xF9, Byte.toUnsignedInt(sha3(256, (byte) 0x30)[0]));
		assertEquals(0x17, Byte.toUnsignedInt(sha3(384, (byte) 0x30)[0]));
		assertEquals(0x2D, Byte.toUnsignedInt(sha3(512, (byte) 0x30)[0]));
		assertEquals(0xD1, Byte.toUnsignedInt(sha3(224, bs)[0]));
		assertEquals(0x69, Byte.toUnsignedInt(sha3(256, bs)[0]));
		assertEquals(0x70, Byte.toUnsignedInt(sha3(384, bs)[0]));
		assertEquals(0x01, Byte.toUnsignedInt(sha3(512, bs)[0]));
		assertEquals(0x04, Byte.toUnsignedInt(sha3(512, lbs)[0]));
	}

	private byte[] sha3(int size, byte b) {
		var sha = new Sha3(size);
		sha.update(b);
		return sha.digest();
	}

	private byte[] sha3(int size, byte[] bs) {
		var sha = new Sha3(size);
		sha.update(bs);
		return sha.digest();
	}

}
