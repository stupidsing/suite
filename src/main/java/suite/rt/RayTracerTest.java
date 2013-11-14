package suite.rt;

import java.io.IOException;

import org.junit.Test;

import suite.math.Vector;

public class RayTracerTest {

	@Test
	public void test() throws IOException {
		AmbientLight lighting = new AmbientLight(new Vector(0f, 0f, -1f), new Vector(0f, 0f, .8f));
		Sphere scene = new Sphere(new Vector(0f, 0f, 5f), 1f);
		new RayTracer(lighting, scene).trace(640, 480, 640);
	}

}
