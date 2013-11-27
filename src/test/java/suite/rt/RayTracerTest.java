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
import suite.rt.RayTracer.LightSource;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.RayTrace;
import suite.rt.composites.Intersect;
import suite.rt.composites.Minus;
import suite.rt.composites.Union;
import suite.rt.planars.Plane;
import suite.rt.planars.Triangle;
import suite.util.Util;

public class RayTracerTest {

	private Vector cr = v(1f, 0f, 0f);
	private Vector cg = v(0f, 1f, 0f);
	private Vector cb = v(0f, 0f, 1f);
	private Vector cy = v(1f, 1f, 0f);
	private Vector cp = v(1f, 0f, 1f);
	private Vector cc = v(0f, 1f, 1f);
	private Vector cw = v(1f, 1f, 1f);

	@Test
	public void testCompositeUnionFail() throws IOException {
		RayTrace sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(0.4f)));
		RayTrace sphere0 = Sphere.c(v(-0.5f, 0f, 5f), 1f, glassy(cb, 0.8f));
		RayTrace sphere1 = Sphere.c(v(0.5f, 0f, 5f), 1f, glassy(cb, 0.8f));
		Scene scene = new Scene(Arrays.asList(sky, new Union(Arrays.asList(sphere0, sphere1))));

		LightSource light = new PointLightSource(v(0f, 0f, 5f), gray(1.5f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rayTracer.test();
	}

	@Test
	public void testBlank() throws IOException {
		RayTracer rayTracer = new RayTracer(Collections.<LightSource> emptySet(), new Scene(Collections.<RayTrace> emptySet()));
		raster(rayTracer);
	}

	@Test
	public void testCompositeIntersect() throws IOException {
		RayTrace sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(0.4f)));
		RayTrace sphere0 = Sphere.c(v(-0.5f, 0f, 5f), 1f, solid(cb));
		RayTrace sphere1 = Sphere.c(v(0.5f, 0f, 5f), 1f, solid(cb));
		Scene scene = new Scene(Arrays.asList(sky, new Intersect(Arrays.asList(sphere0, sphere1))));

		LightSource light = new PointLightSource(v(-10f, -10f, -7f), gray(1.5f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer);
	}

	@Test
	public void testCompositeMinus() throws IOException {
		RayTrace sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(0.4f)));
		RayTrace sphere0 = Sphere.c(v(-0.5f, 0f, 5f), 1f, solid(cb));
		RayTrace sphere1 = Sphere.c(v(0.5f, 0f, 4f), 1f, solid(cb));
		Scene scene = new Scene(Arrays.asList(sky, new Minus(sphere0, sphere1)));

		LightSource light = new PointLightSource(v(-10f, -10f, -7f), gray(1.5f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer);
	}

	@Test
	public void testCompositeUnion() throws IOException {
		RayTrace sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(0.4f)));
		RayTrace sphere0 = Sphere.c(v(-0.5f, 0f, 5f), 1f, solid(cb));
		RayTrace sphere1 = Sphere.c(v(0.5f, 0f, 5f), 1f, solid(cb));
		Scene scene = new Scene(Arrays.asList(sky, new Union(Arrays.asList(sphere0, sphere1))));

		LightSource light = new PointLightSource(v(-10f, -10f, -7f), gray(1.5f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer);
	}

	@Test
	public void testMess() throws IOException {
		RayTrace sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(0.4f)));
		RayTrace sphere0 = Sphere.c(v(1f, -1f, 4f), 1f, glassy(cr, 1f));
		RayTrace sphere1 = Sphere.c(v(0f, 0f, 6f), 1f, glassy(cg, 1f));
		RayTrace sphere2 = Sphere.c(v(-1f, 1f, 8f), 1f, glassy(cb, 1f));
		RayTrace plane0 = new Plane(v(0f, -1f, 0f), 20f, solid(cy));
		RayTrace triangle = Triangle.c(v(0.5f, 0.5f, 3f), v(0.5f, 0f, 0f), v(0f, 0.5f, 0f), glassy(cc, 1f));
		Scene scene = new Scene(Arrays.asList(sky, sphere0, sphere1, sphere2, plane0, triangle));

		LightSource light0 = new PointLightSource(v(10f, 10f, -10f), cp);
		LightSource light1 = new PointLightSource(v(-10f, 10f, -10f), gray(1f));
		List<LightSource> lights = Arrays.asList(light0, light1);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer);
	}

	@Test
	public void testSphereMirror() throws IOException {
		RayTrace sphere = Sphere.c(v(0f, 0f, 3f), 1f, solid(cb));
		RayTrace mirror = new Plane(v(1f, 0f, 0f), -0.3f, material(cw, 1f, 0f));
		Scene scene = new Scene(Arrays.asList(sphere, mirror));

		LightSource light = new PointLightSource(v(10000f, 10000f, -10000f), gray(1.5f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer);
	}

	@Test
	public void testSphereReflection() throws IOException {
		RayTrace sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(0.4f)));
		RayTrace sphere = Sphere.c(v(0f, 0f, 3f), 1f, material(cb, 1f, 0f));
		Scene scene = new Scene(Arrays.asList(sky, sphere));

		LightSource light = new PointLightSource(v(-10f, -10f, -7f), gray(1.5f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer);
	}

	@Test
	public void testSphereRefraction() throws IOException {
		RayTrace sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(0.4f)));
		RayTrace sphere = Sphere.c(v(0f, 0f, 3f), 1f, material(cb, 0f, 1f));
		Scene scene = new Scene(Arrays.asList(sky, sphere));

		LightSource light = new PointLightSource(v(-10f, -10f, -7f), gray(1.5f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer);
	}

	@Test
	public void testSphereSolid() throws IOException {
		RayTrace sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(0.4f)));
		RayTrace sphere = Sphere.c(v(0f, 0f, 3f), 1f, solid(cb));
		Scene scene = new Scene(Arrays.asList(sky, sphere));

		LightSource light = new PointLightSource(v(-10f, -10f, -7f), gray(1.5f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer);
	}

	@Test
	public void testSpheres() throws IOException {
		RayTrace sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(0.4f)));
		RayTrace sphere0 = Sphere.c(v(-1.5f, 0f, 5f), 1f, glassy(cb, 0.8f));
		RayTrace sphere1 = Sphere.c(v(1.5f, 0f, 5f), 1f, glassy(cb, 0.8f));
		Scene scene = new Scene(Arrays.asList(sky, sphere0, sphere1));

		LightSource light = new PointLightSource(v(0f, 0f, 5f), gray(1.5f));
		List<LightSource> lights = Arrays.asList(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		raster(rayTracer);
	}

	private void raster(RayTracer rayTracer) throws IOException {
		String filename = "/tmp/" + Util.getStackTrace(3).getMethodName() + ".png";

		BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
		rayTracer.trace(bufferedImage, 640);
		ImageIO.write(bufferedImage, "png", new File(filename));
	}

	private Material solid(Vector color) {
		return material(color, 0f, 0f);
	}

	private Material glassy(Vector color, float index) {
		return material(color, index, index);
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
