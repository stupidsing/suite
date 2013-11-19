package suite.rt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

import suite.math.Vector;
import suite.rt.RayTracer.LightSource;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.RayTraceObject;

public class RayTracerTest {

	private Vector RED__ = new Vector(1f, 0f, 0f);
	private Vector GREEN = new Vector(0f, 1f, 0f);
	private Vector BLUE_ = new Vector(0f, 0f, 1f);

	@Test
	public void test() throws IOException {
		RayTraceObject sphere0 = new Sphere(new Vector(-1f, -1f, 4f), 1f, reflective(RED__, 0.4f));
		RayTraceObject sphere1 = new Sphere(new Vector(0f, 0f, 6f), 1f, reflective(GREEN, 0.4f));
		RayTraceObject sphere2 = new Sphere(new Vector(1f, 1f, 8f), 1f, reflective(BLUE_, 0.4f));
		RayTraceObject plane = new Plane(new Vector(0f, 1f, 0f), -5f, white());
		Scene scene = new Scene(Arrays.asList(sphere0, sphere1, sphere2, plane));

		LightSource light = new PointLightSource(new Vector(10000f, 10000f, -10000f), new Vector(1, 1, 1f));
		List<LightSource> lights = Arrays.asList(light);
		RayTracer rayTracer = new RayTracer(lights, scene);

		BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		rayTracer.trace(bufferedImage, 640);

		ImageIO.write(bufferedImage, "png", new File("/tmp/ray-tracer.png"));
	}

	private Material reflective(Vector color, float index) {
		float reflectionIndex1 = 1 - index;
		final Vector litIndex = Vector.mul(color, index);
		final Vector reflectionIndex = new Vector(reflectionIndex1, reflectionIndex1, reflectionIndex1);

		return new Material() {
			public Vector litIndex() {
				return litIndex;
			}

			public Vector reflectionIndex() {
				return reflectionIndex;
			}

			public Vector refractionIndex() {
				return new Vector(0f, 0f, 0f);
			}
		};
	}

	private Material white() {
		return new Material() {
			public Vector litIndex() {
				return new Vector(0.5f, 0.5f, 0.5f);
			}

			public Vector reflectionIndex() {
				return new Vector(0.5f, 0.5f, 0.5f);
			}

			public Vector refractionIndex() {
				return new Vector(0f, 0f, 0f);
			}
		};
	}

}
