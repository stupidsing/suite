package suite.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class Sha3Test {

	@Test
	public void test() {
		var longString = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

		var empty = new byte[0];
		var qbfbs = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.US_ASCII);
		var lbs = longString.getBytes(StandardCharsets.US_ASCII);

		assertEquals(0x2D, Byte.toUnsignedInt(sha3(512, new byte[] { (byte) 0x30 })[0]));
		assertEquals(0x7E, Byte.toUnsignedInt(sha3(512, "xxxxxxxx".getBytes(StandardCharsets.US_ASCII))[0]));

		assertEquals(0x6B, Byte.toUnsignedInt(sha3(224, empty)[0]));
		assertEquals(0xA7, Byte.toUnsignedInt(sha3(256, empty)[0]));
		assertEquals(0x0C, Byte.toUnsignedInt(sha3(384, empty)[0]));
		assertEquals(0xA6, Byte.toUnsignedInt(sha3(512, empty)[0]));
		assertEquals(0xA8, Byte.toUnsignedInt(sha3(224, (byte) 0x30)[0]));
		assertEquals(0xF9, Byte.toUnsignedInt(sha3(256, (byte) 0x30)[0]));
		assertEquals(0x17, Byte.toUnsignedInt(sha3(384, (byte) 0x30)[0]));
		assertEquals(0x2D, Byte.toUnsignedInt(sha3(512, (byte) 0x30)[0]));
		assertEquals(0xD1, Byte.toUnsignedInt(sha3(224, qbfbs)[0]));
		assertEquals(0x69, Byte.toUnsignedInt(sha3(256, qbfbs)[0]));
		assertEquals(0x70, Byte.toUnsignedInt(sha3(384, qbfbs)[0]));
		assertEquals(0x01, Byte.toUnsignedInt(sha3(512, qbfbs)[0]));
		assertEquals(0x04, Byte.toUnsignedInt(sha3(512, lbs)[0]));
	}

	@Test
	public void test0() {
		var sha = new Sha3(512);
		sha.update("x".getBytes(StandardCharsets.US_ASCII));
		sha.update("xxxxxxxxy".getBytes(StandardCharsets.US_ASCII));
		assertEquals(0x1F, Byte.toUnsignedInt(sha.digest()[0]));
	}

	private byte[] sha3(int size, byte b) {
		var sha = new Sha3(size);
		sha.updateBits(b, 8);
		return sha.digest();
	}

	private byte[] sha3(int size, byte[] bs) {
		var sha = new Sha3(size);
		sha.update(bs);
		return sha.digest();
	}

}
