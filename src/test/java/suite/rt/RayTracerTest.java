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

	@Test
	public void testBlank() throws IOException {
		RayTracer rayTracer = new RayTracer(Collections.<LightSource> emptySet(),
				new Scene(Collections.<RayTraceObject> emptySet()));

		BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		rayTracer.trace(bufferedImage, 640);
		ImageIO.write(bufferedImage, "png", new File("/tmp/ray-tracer-blank.png"));
	}

	@Test
	public void testSphere() throws IOException {
		RayTraceObject sphere = Sphere.c(v(0f, 0f, 3f), 1f, reflective(cr, 0.4f));
		Scene scene = new Scene(Arrays.asList(sphere));

		LightSource light = new PointLightSource(v(10000f, 10000f, -10000f), gray(1f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);

		BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		rayTracer.trace(bufferedImage, 500);
		ImageIO.write(bufferedImage, "png", new File("/tmp/ray-tracer-sphere.png"));
	}

	@Test
	public void testSpheres() throws IOException {
		RayTraceObject sphere0 = Sphere.c(v(-2f, 0f, 5f), 1f, reflective(cr, 0.4f));
		RayTraceObject sphere1 = Sphere.c(v(2f, 0f, 5f), 1f, reflective(cr, 0.4f));
		Scene scene = new Scene(Arrays.asList(sphere0, sphere1));

		LightSource light = new PointLightSource(v(10000f, 10000f, -10000f), gray(1f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);

		BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		rayTracer.trace(bufferedImage, 500);
		ImageIO.write(bufferedImage, "png", new File("/tmp/ray-tracer-spheres.png"));
	}

	@Test
	public void testMess() throws IOException {
		RayTraceObject sphere0 = Sphere.c(v(1f, -1f, 3f), 1f, reflective(cr, 0.4f));
		RayTraceObject sphere1 = Sphere.c(v(0f, 0f, 5f), 1f, reflective(cg, 0.4f));
		RayTraceObject sphere2 = Sphere.c(v(-1f, 1f, 7f), 1f, reflective(cb, 0.4f));
		RayTraceObject plane = new Plane(v(0f, 1f, 0f), -3f, white());
		RayTraceObject triangle = new Triangle(v(0.2f, 0.2f, 3f), v(0.2f, 0f, 0f), v(0f, 0.2f, 0f), white());
		Scene scene = new Scene(Arrays.asList(sphere0, sphere1, sphere2, plane, triangle));

		LightSource light0 = new PointLightSource(v(10000f, 10000f, -10000f), gray(1f));
		List<LightSource> lights = Arrays.asList(light0);

		RayTracer rayTracer = new RayTracer(lights, scene);

		BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		rayTracer.trace(bufferedImage, 500);
		ImageIO.write(bufferedImage, "png", new File("/tmp/ray-tracer.png"));
	}

	private Material reflective(Vector color, float index) {
		float index1 = 1f - index;
		final Vector litIndex = Vector.mul(color, index1);
		final Vector reflectionIndex = gray(index);

		return new Material() {
			public Vector litIndex() {
				return litIndex;
			}

			public Vector reflectionIndex() {
				return reflectionIndex;
			}

			public Vector refractionIndex() {
				return gray(0f);
			}
		};
	}

	private Material white() {
		return new Material() {
			public Vector litIndex() {
				return gray(0.6f);
			}

			public Vector reflectionIndex() {
				return gray(0.4f);
			}

			public Vector refractionIndex() {
				return gray(0f);
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
