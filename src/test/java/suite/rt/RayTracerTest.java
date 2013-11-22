package suite.rt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

import suite.math.Vector;
import suite.rt.Planar.Triangle;
import suite.rt.RayTracer.LightSource;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.RayTraceObject;

public class RayTracerTest {

	private Vector cr = v(1f, 0f, 0f);
	private Vector cg = v(0f, 1f, 0f);
	private Vector cb = v(0f, 0f, 1f);
	private Vector cy = v(1f, 1f, 0f);
	private Vector cp = v(1f, 0f, 1f);
	private Vector cc = v(0f, 1f, 1f);
	private Vector cw = v(1f, 1f, 1f);

	@Test
	public void testBlank() throws IOException {
		RayTracer rayTracer = new RayTracer(Collections.<LightSource> emptySet(),
				new Scene(Collections.<RayTraceObject> emptySet()));
		raster(rayTracer, "/tmp/ray-tracer-blank.png");
	}

	@Test
	public void testSphere() throws IOException {
		RayTraceObject sphere = Sphere.c(v(0f, 0f, 3f), 1f, glassy(cr, 0.3f));
		Scene scene = new Scene(Arrays.asList(sphere));

		LightSource light = new PointLightSource(v(-10000f, -10000f, -10000f), gray(1f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer, "/tmp/ray-tracer-sphere.png");
	}

	@Test
	public void testSphereReflection() throws IOException {
		RayTraceObject sphere = Sphere.c(v(0f, 0f, 3f), 1f, solid(cr));
		RayTraceObject mirror = new Plane(v(1f, 0f, 0f), -0.3f, glassy(cw, 0.8f));
		Scene scene = new Scene(Arrays.asList(sphere, mirror));

		LightSource light = new PointLightSource(v(10000f, 10000f, -10000f), gray(1f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer, "/tmp/ray-tracer-sphere-reflection.png");
	}

	@Test
	public void testSpheres() throws IOException {
		RayTraceObject sphere0 = Sphere.c(v(-2f, 0f, 5f), 1f, glassy(cr, 0.8f));
		RayTraceObject sphere1 = Sphere.c(v(2f, 0f, 5f), 1f, glassy(cr, 0.8f));
		Scene scene = new Scene(Arrays.asList(sphere0, sphere1));

		LightSource light = new PointLightSource(v(0f, 0f, 5f), gray(1f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);

		raster(rayTracer, "/tmp/ray-tracer-spheres.png");
	}

	@Test
	public void testMess() throws IOException {
		RayTraceObject sphere0 = Sphere.c(v(1f, -1f, 4f), 1f, glassy(cr, 0.5f));
		RayTraceObject sphere1 = Sphere.c(v(0f, 0f, 6f), 1f, glassy(cg, 0.5f));
		RayTraceObject sphere2 = Sphere.c(v(-1f, 1f, 8f), 1f, glassy(cb, 0.5f));
		RayTraceObject plane0 = new Plane(v(0f, -1f, 0f), 20f, solid(cy));
		RayTraceObject triangle = new Triangle(v(0.2f, 0.2f, 3f), v(0.2f, 0f, 0f), v(0f, 0.2f, 0f), glassy(cc, 0.8f));
		Scene scene = new Scene(Arrays.asList(sphere0, sphere1, sphere2, plane0, triangle));

		LightSource light0 = new PointLightSource(v(10000f, 10000f, -10000f), cp);
		LightSource light1 = new PointLightSource(v(-10000f, 10000f, -10000f), gray(10f));
		List<LightSource> lights = Arrays.asList(light0, light1);

		RayTracer rayTracer = new RayTracer(lights, scene);

		raster(rayTracer, "/tmp/ray-tracer.png");
	}

	private void raster(RayTracer rayTracer, String filename) throws IOException {
		BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		rayTracer.trace(bufferedImage, 640);
		ImageIO.write(bufferedImage, "png", new File(filename));
	}

	private Material solid(Vector color) {
		return material(color, 0f, 0f);
	}

	private Material glassy(Vector color, float index) {
		return material(color, index, 1 - index);
	}

	private Material material(final Vector color, final float reflectionIndex, final float refractionIndex) {
		return new Material() {
			public Vector surfaceColor() {
				return color;
			}

			public float reflectionIndex() {
				return reflectionIndex;
			}

			public float refractionIndex() {
				return refractionIndex;
			}
		};
	}

	private Vector gray(float f) {
		return v(f, f, f);
	}

	private Vector v(float x, float y, float z) {
		return new Vector(x, y, z);
	}

}
