package suite.sample;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConwayGameOfLifeTest {

	@Test
	public void testGlider() {
		ConwayGameOfLife cgol = new ConwayGameOfLife("" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　Ｏ　　　　　　　　　　　　　\n" //
				+ "　　　Ｏ　　　　　　　　　　　　\n" //
				+ "　ＯＯＯ　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
		);

		assertEquals(5, cgol.population());

		for (int i = 0; i < 32; i++) {
			cgol = cgol.evolve(cgol);
			System.out.println(cgol);
		}

		assertEquals(5, cgol.population());
	}

}
