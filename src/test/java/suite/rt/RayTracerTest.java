package suite.rt;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import suite.math.Vector;
import suite.rt.RayTracer.LightSource;
import suite.rt.RayTracer.RayTraceObject;

public class RayTracerTest {

	@Test
	public void test() throws IOException {
		RayTraceObject sphere0 = new Sphere(new Vector(-1f, -1f, 4f), 1f);
		RayTraceObject sphere1 = new Sphere(new Vector(0f, 0f, 6f), 1f);
		RayTraceObject sphere2 = new Sphere(new Vector(1f, 1f, 8f), 1f);
		RayTraceObject plane = new Plane(new Vector(0f, 1f, 0f), -5f);

		LightSource light = new PointLight(new Vector(10000f, 10000f, -10000f), new Vector(1, 1, 1f));
		Scene scene = new Scene(Arrays.asList(sphere0, sphere1, sphere2, plane));

		new RayTracer(Arrays.asList(light), scene).trace(640, 480, 640);
	}

}
