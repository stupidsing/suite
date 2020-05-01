package suite.math;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Sha2Test {

	private Sha2 sha2 = new Sha2();

	@Test
	public void test() {
		byte[] empty = new byte[0];
		assertEquals(0xE3, Byte.toUnsignedInt(sha2.sha256(empty)[0]));
		assertEquals(0x4B, Byte.toUnsignedInt(sha2.sha256(new byte[] { 1, })[0]));
		assertEquals(0xB6, Byte.toUnsignedInt(sha2.hmacSha256(empty, empty)[0]));
		assertEquals(0xF7, Byte.toUnsignedInt(sha2.hmacSha256("key".getBytes(StandardCharsets.US_ASCII),
				"The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.US_ASCII))[0]));
	}

}
