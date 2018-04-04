package suite.streamlet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StreamletTest {

	@Test
	public void testChunk() {
		var objects = new Object[29];

		for (int i = 0; i < objects.length; i++)
			objects[i] = new Object();

		Outlet<Outlet<Object>> chunks = Outlet.of(objects).chunk(5);
		assertEquals(5, chunks.next().toList().size());
		assertEquals(5, chunks.next().toList().size());
		assertEquals(5, chunks.next().toList().size());
		assertEquals(5, chunks.next().toList().size());
		assertEquals(5, chunks.next().toList().size());
		assertEquals(4, chunks.next().toList().size());
	}

}
