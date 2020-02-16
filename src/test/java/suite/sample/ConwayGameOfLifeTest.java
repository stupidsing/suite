package suite.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ConwayGameOfLifeTest {

	@Test
	public void testGlider() {
		var cgol = new ConwayGameOfLife("" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　Ｏ　　　　　　　　　　　　　\n" //
				+ "　　　Ｏ　　　　　　　　　　　　\n" //
				+ "　ＯＯＯ　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
		);

		cgol = evolve(cgol, 32);
		assertEquals(5, cgol.population());
	}

	@Test
	public void testSpaceship() {
		var cgol = new ConwayGameOfLife("" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　Ｏ　　Ｏ　　　　　　　　　　　\n" //
				+ "　　　　　Ｏ　　　　　　　　　　\n" //
				+ "　Ｏ　　　Ｏ　　　　　　　　　　\n" //
				+ "　　ＯＯＯＯ　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
				+ "　　　　　　　　　　　　　　　　\n" //
		);

		cgol = evolve(cgol, 16);
		assertEquals(9, cgol.population());
	}

	private ConwayGameOfLife evolve(ConwayGameOfLife cgol, int times) {
		for (var i = 0; i < times; i++) {
			cgol = cgol.evolve(cgol);
			System.out.println(cgol);
		}
		return cgol;
	}

}
