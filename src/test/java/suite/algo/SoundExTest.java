package suite.algo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SoundExTest {

	private SoundEx soundEx = new SoundEx();

	@Test
	public void test() {
		assertEquals("A261", soundEx.american("Ashcroft"));
		assertEquals("D253", soundEx.american("DeSmet"));
		assertEquals("G362", soundEx.american("Gutierrez"));
		assertEquals("J250", soundEx.american("Jackson"));
		assertEquals("P236", soundEx.american("Pfister"));
		assertEquals("T522", soundEx.american("Tymczak"));
		assertEquals("W252", soundEx.american("Washington"));
		assertEquals("W000", soundEx.american("Wu"));
	}

}
