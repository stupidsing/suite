package suite.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Sha2Test {

	@Test
	public void test() {
		assertEquals(0xE3, Byte.toUnsignedInt(new Sha2().sha256(new byte[0])[0]));
	}

}
