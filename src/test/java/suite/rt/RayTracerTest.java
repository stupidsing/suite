package suite.rt;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.Test;

import suite.Constants;
import suite.math.Vector;
import suite.os.FileUtil;
import suite.rt.RayTracer.LightSource;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.RtObject;
import suite.rt.composite.Intersect;
import suite.rt.composite.Minus;
import suite.rt.composite.Union;
import suite.rt.planar.Plane;
import suite.rt.planar.Triangle;
import suite.util.Thread_;

public class RayTracerTest {

	private Vector cr = v(1f, .4f, .4f);
	private Vector cg = v(.4f, 1f, .4f);
	private Vector cb = v(.4f, .4f, 1f);
	private Vector cy = v(1f, 1f, .4f);
	private Vector cp = v(1f, .4f, 1f);
	private Vector cc = v(.4f, 1f, 1f);
	private Vector cw = v(1f, 1f, 1f);

	@Test
	public void testBlank() throws IOException {
		RayTracer rayTracer = new RayTracer(Set.of(), new Scene(Set.of()));
		rasterize(rayTracer);
	}

	@Test
	public void testCompositeIntersect() throws IOException {
		RtObject sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(.4f)));
		RtObject sphere0 = Sphere.c(v(-.5f, 0f, 5f), 1f, solid(cb));
		RtObject sphere1 = Sphere.c(v(.5f, 0f, 5f), 1f, solid(cb));
		Scene scene = new Scene(List.of(sky, new Intersect(List.of(sphere0, sphere1))));

		LightSource light = new PointLightSource(v(-10f, -10f, -7f), gray(1.5f));
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testCompositeMinus() throws IOException {
		RtObject sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(.4f)));
		RtObject sphere0 = Sphere.c(v(-.5f, 0f, 5f), 1f, solid(cb));
		RtObject sphere1 = Sphere.c(v(.5f, 0f, 4f), 1f, solid(cb));
		Scene scene = new Scene(List.of(sky, new Minus(sphere0, sphere1)));

		LightSource light = new PointLightSource(v(-10f, -10f, -7f), gray(1.5f));
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testCompositeUnion() throws IOException {
		RtObject sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(.4f)));
		RtObject sphere0 = Sphere.c(v(-.5f, 0f, 5f), 1f, solid(cb));
		RtObject sphere1 = Sphere.c(v(.5f, 0f, 5f), 1f, solid(cb));
		Scene scene = new Scene(List.of(sky, new Union(List.of(sphere0, sphere1))));

		LightSource light = new PointLightSource(v(-10f, -10f, -7f), gray(1.5f));
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testCopyCat() throws IOException {
		RtObject sphere0 = Sphere.c(v(0f, 1004f, 20f), 1000f, solid(v(.2f, .2f, .2f)));
		RtObject sphere1 = Sphere.c(v(0f, 0f, 20f), 4f, glassy(v(1f, .32f, .36f)));
		RtObject sphere2 = Sphere.c(v(5f, 1f, 15f), 2f, reflective(v(.9f, .76f, .46f)));
		RtObject sphere3 = Sphere.c(v(5f, 0f, 25f), 3f, reflective(v(.65f, .77f, .97f)));
		RtObject sphere4 = Sphere.c(v(-5.5f, 0f, 15f), 3f, reflective(gray(.9f)));
		// rtObject sphere5 =
		// sphere.c(v(0f, -20f, 30f), 3f, solid(v(0f, 0f, 0f)));

		Scene scene = new Scene(List.of(sphere0, sphere1, sphere2, sphere3, sphere4));

		LightSource light0 = new PointLightSource(v(0f, -20f, 30f), gray(3f));
		List<LightSource> lights = List.of(light0);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rayTracer.setAmbient(gray(2f));

		rasterize(rayTracer);
	}

	@Test
	public void testLight() throws IOException {
		RtObject sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(cw));
		Scene scene = new Scene(List.of(sky));

		LightSource light = new PointLightSource(v(0f, 0f, 90f), cw);
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testMess() throws IOException {
		RtObject sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(.4f)));
		RtObject sphere0 = Sphere.c(v(1f, -1f, 4f), 1f, glassy(cr));
		RtObject sphere1 = Sphere.c(v(0f, 0f, 6f), 1f, glassy(cg));
		RtObject sphere2 = Sphere.c(v(-1f, 1f, 8f), 1f, glassy(cb));
		RtObject plane0 = new Plane(v(0f, -1f, 0f), 20f, solid(cy));
		RtObject triangle = Triangle.c(v(.5f, .5f, 3f), v(.5f, 0f, 0f), v(0f, .5f, 0f), glassy(cc));
		Scene scene = new Scene(List.of(sky, sphere0, sphere1, sphere2, plane0, triangle));

		LightSource light0 = new PointLightSource(v(10f, 10f, -10f), cp);
		LightSource light1 = new PointLightSource(v(-10f, 10f, -10f), gray(2f));
		List<LightSource> lights = List.of(light0, light1);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereMirror() throws IOException {
		RtObject sphere = Sphere.c(v(0f, 0f, 3f), 1f, solid(cb));
		RtObject mirror = new Plane(v(1f, 0f, 0f), -.3f, glassy(cw));
		Scene scene = new Scene(List.of(sphere, mirror));

		LightSource light = new PointLightSource(v(10000f, 10000f, -10000f), gray(1.5f));
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereReflection() throws IOException {
		RtObject sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(cw));
		RtObject sphere = Sphere.c(v(0f, 0f, 3f), 1f, reflective(cb));
		Scene scene = new Scene(List.of(sky, sphere));

		LightSource light = new PointLightSource(v(0f, 0f, 90f), cw);
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereRefraction() throws IOException {
		RtObject sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(cw));
		RtObject sphere = Sphere.c(v(0f, 0f, 3f), 1f, glassy(cb));
		Scene scene = new Scene(List.of(sky, sphere));

		LightSource light = new PointLightSource(v(0f, 0f, 90f), cw);
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereSolid() throws IOException {
		RtObject sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(cw));
		RtObject sphere = Sphere.c(v(0f, 0f, 3f), 1f, solid(cb));
		Scene scene = new Scene(List.of(sky, sphere));

		LightSource light = new PointLightSource(v(0f, 0f, 90f), cw);
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSpheres() throws IOException {
		RtObject sky = Sphere.c(v(0f, 0f, 0f), 100f, solid(gray(.4f)));
		RtObject sphere0 = Sphere.c(v(-1.5f, 0f, 5f), 1f, glassy(cb));
		RtObject sphere1 = Sphere.c(v(1.5f, 0f, 5f), 1f, glassy(cb));
		Scene scene = new Scene(List.of(sky, sphere0, sphere1));

		LightSource light = new PointLightSource(v(0f, 0f, 5f), gray(1.5f));
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	private void rasterize(RayTracer rayTracer) throws IOException {
		Path path = Constants.tmp(Thread_.getStackTrace(3).getMethodName() + ".png");
		BufferedImage bufferedImage = rayTracer.trace(640, 480, 640);

		try (OutputStream os = FileUtil.out(path)) {
			ImageIO.write(bufferedImage, "png", os);
		}
	}

	private Material solid(Vector color) {
		return material(color, false, false);
	}

	private Material glassy(Vector color) {
		return material(color, true, true);
	}

	private Material reflective(Vector color) {
		return material(color, true, false);
	}

	private Material material(Vector color, boolean isReflective, boolean isTransparent) {
		return new Material() {
			public Vector surfaceColor() {
				return color;
			}

			public boolean isReflective() {
				return isReflective;
			}

			public float transparency() {
				return isTransparent ? .5f : 0f;
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
