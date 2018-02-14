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

	private Vector cr = v(1d, .4d, .4d);
	private Vector cg = v(.4d, 1d, .4d);
	private Vector cb = v(.4d, .4d, 1d);
	private Vector cy = v(1d, 1d, .4d);
	private Vector cp = v(1d, .4d, 1d);
	private Vector cc = v(.4d, 1d, 1d);
	private Vector cw = v(1d, 1d, 1d);

	@Test
	public void testBlank() throws IOException {
		RayTracer rayTracer = new RayTracer(Set.of(), new Scene(Set.of()));
		rasterize(rayTracer);
	}

	@Test
	public void testCompositeIntersect() throws IOException {
		RtObject sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		RtObject sphere0 = Sphere.c(v(-.5d, 0d, 5d), 1d, solid(cb));
		RtObject sphere1 = Sphere.c(v(.5d, 0d, 5d), 1d, solid(cb));
		Scene scene = new Scene(List.of(sky, new Intersect(List.of(sphere0, sphere1))));

		LightSource light = new PointLightSource(v(-10d, -10d, -7d), gray(1.5d));
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testCompositeMinus() throws IOException {
		RtObject sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		RtObject sphere0 = Sphere.c(v(-.5d, 0d, 5d), 1d, solid(cb));
		RtObject sphere1 = Sphere.c(v(.5d, 0d, 4d), 1d, solid(cb));
		Scene scene = new Scene(List.of(sky, new Minus(sphere0, sphere1)));

		LightSource light = new PointLightSource(v(-10d, -10d, -7d), gray(1.5d));
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testCompositeUnion() throws IOException {
		RtObject sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		RtObject sphere0 = Sphere.c(v(-.5d, 0d, 5d), 1d, solid(cb));
		RtObject sphere1 = Sphere.c(v(.5d, 0d, 5d), 1d, solid(cb));
		Scene scene = new Scene(List.of(sky, new Union(List.of(sphere0, sphere1))));

		LightSource light = new PointLightSource(v(-10d, -10d, -7d), gray(1.5d));
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testCopyCat() throws IOException {
		RtObject sphere0 = Sphere.c(v(0d, 1004d, 20d), 1000d, solid(v(.2d, .2d, .2d)));
		RtObject sphere1 = Sphere.c(v(0d, 0d, 20d), 4d, glassy(v(1d, .32d, .36d)));
		RtObject sphere2 = Sphere.c(v(5d, 1d, 15d), 2d, reflective(v(.9d, .76d, .46d)));
		RtObject sphere3 = Sphere.c(v(5d, 0d, 25d), 3d, reflective(v(.65d, .77d, .97d)));
		RtObject sphere4 = Sphere.c(v(-5.5d, 0d, 15d), 3d, reflective(gray(.9d)));
		// rtObject sphere5 =
		// sphere.c(v(0d, -20d, 30d), 3d, solid(v(0d, 0d, 0d)));

		Scene scene = new Scene(List.of(sphere0, sphere1, sphere2, sphere3, sphere4));

		LightSource light0 = new PointLightSource(v(0d, -20d, 30d), gray(3d));
		List<LightSource> lights = List.of(light0);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rayTracer.setAmbient(gray(2d));

		rasterize(rayTracer);
	}

	@Test
	public void testLight() throws IOException {
		RtObject sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(cw));
		Scene scene = new Scene(List.of(sky));

		LightSource light = new PointLightSource(v(0d, 0d, 90d), cw);
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testMess() throws IOException {
		RtObject sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		RtObject sphere0 = Sphere.c(v(1d, -1d, 4d), 1d, glassy(cr));
		RtObject sphere1 = Sphere.c(v(0d, 0d, 6d), 1d, glassy(cg));
		RtObject sphere2 = Sphere.c(v(-1d, 1d, 8d), 1d, glassy(cb));
		RtObject plane0 = new Plane(v(0d, -1d, 0d), 20d, solid(cy));
		RtObject triangle = Triangle.c(v(.5d, .5d, 3d), v(.5d, 0d, 0d), v(0d, .5d, 0d), glassy(cc));
		Scene scene = new Scene(List.of(sky, sphere0, sphere1, sphere2, plane0, triangle));

		LightSource light0 = new PointLightSource(v(10d, 10d, -10d), cp);
		LightSource light1 = new PointLightSource(v(-10d, 10d, -10d), gray(2d));
		List<LightSource> lights = List.of(light0, light1);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereMirror() throws IOException {
		RtObject sphere = Sphere.c(v(0d, 0d, 3d), 1d, solid(cb));
		RtObject mirror = new Plane(v(1d, 0d, 0d), -.3d, glassy(cw));
		Scene scene = new Scene(List.of(sphere, mirror));

		LightSource light = new PointLightSource(v(10000d, 10000d, -10000d), gray(1.5d));
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereReflection() throws IOException {
		RtObject sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(cw));
		RtObject sphere = Sphere.c(v(0d, 0d, 3d), 1d, reflective(cb));
		Scene scene = new Scene(List.of(sky, sphere));

		LightSource light = new PointLightSource(v(0d, 0d, 90d), cw);
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereRefraction() throws IOException {
		RtObject sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(cw));
		RtObject sphere = Sphere.c(v(0d, 0d, 3d), 1d, glassy(cb));
		Scene scene = new Scene(List.of(sky, sphere));

		LightSource light = new PointLightSource(v(0d, 0d, 90d), cw);
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereSolid() throws IOException {
		RtObject sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(cw));
		RtObject sphere = Sphere.c(v(0d, 0d, 3d), 1d, solid(cb));
		Scene scene = new Scene(List.of(sky, sphere));

		LightSource light = new PointLightSource(v(0d, 0d, 90d), cw);
		List<LightSource> lights = List.of(light);

		RayTracer rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSpheres() throws IOException {
		RtObject sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		RtObject sphere0 = Sphere.c(v(-1.5d, 0d, 5d), 1d, glassy(cb));
		RtObject sphere1 = Sphere.c(v(1.5d, 0d, 5d), 1d, glassy(cb));
		Scene scene = new Scene(List.of(sky, sphere0, sphere1));

		LightSource light = new PointLightSource(v(0d, 0d, 5d), gray(1.5d));
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

			public double transparency() {
				return isTransparent ? .5d : 0d;
			}
		};
	}

	private Vector gray(double f) {
		return v(f, f, f);
	}

	private Vector v(double x, double y, double z) {
		return new Vector(x, y, z);
	}

}
