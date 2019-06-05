package suite.rt;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.Test;

import suite.cfg.Defaults;
import suite.editor.ImageViewer;
import suite.math.R3;
import suite.os.FileUtil;
import suite.rt.RayTracer.LightSource;
import suite.rt.RayTracer.Material;
import suite.rt.composite.Intersect;
import suite.rt.composite.Minus;
import suite.rt.composite.Union;
import suite.rt.planar.Plane;
import suite.rt.planar.Triangle;
import suite.util.Thread_;

public class RayTracerTest {

	private R3 cr = v(1d, .4d, .4d);
	private R3 cg = v(.4d, 1d, .4d);
	private R3 cb = v(.4d, .4d, 1d);
	private R3 cy = v(1d, 1d, .4d);
	private R3 cp = v(1d, .4d, 1d);
	private R3 cc = v(.4d, 1d, 1d);
	private R3 cw = v(1d, 1d, 1d);

	@Test
	public void testBlank() throws IOException {
		var rayTracer = new RayTracer(Set.of(), new Scene(Set.of()));
		rasterize(rayTracer);
	}

	@Test
	public void testCompositeIntersect() throws IOException {
		var sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		var sphere0 = Sphere.c(v(-.5d, 0d, 5d), 1d, solid(cb));
		var sphere1 = Sphere.c(v(.5d, 0d, 5d), 1d, solid(cb));
		var scene = new Scene(List.of(sky, new Intersect(List.of(sphere0, sphere1))));

		var light = new PointLightSource(v(-10d, -10d, -7d), gray(1.5d));
		var lights = lights(light);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testCompositeMinus() throws IOException {
		var sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		var sphere0 = Sphere.c(v(-.5d, 0d, 5d), 1d, solid(cb));
		var sphere1 = Sphere.c(v(.5d, 0d, 4d), 1d, solid(cb));
		var scene = new Scene(List.of(sky, new Minus(sphere0, sphere1)));

		var light = new PointLightSource(v(-10d, -10d, -7d), gray(1.5d));
		var lights = lights(light);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testCompositeUnion() throws IOException {
		var sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		var sphere0 = Sphere.c(v(-.5d, 0d, 5d), 1d, solid(cb));
		var sphere1 = Sphere.c(v(.5d, 0d, 5d), 1d, solid(cb));
		var scene = new Scene(List.of(sky, new Union(List.of(sphere0, sphere1))));

		var light = new PointLightSource(v(-10d, -10d, -7d), gray(1.5d));
		var lights = lights(light);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testCopyCat() throws IOException {
		var sphere0 = Sphere.c(v(0d, 1004d, 20d), 1000d, solid(v(.2d, .2d, .2d)));
		var sphere1 = Sphere.c(v(0d, 0d, 20d), 4d, glassy(v(1d, .32d, .36d)));
		var sphere2 = Sphere.c(v(5d, 1d, 15d), 2d, reflective(v(.9d, .76d, .46d)));
		var sphere3 = Sphere.c(v(5d, 0d, 25d), 3d, reflective(v(.65d, .77d, .97d)));
		var sphere4 = Sphere.c(v(-5.5d, 0d, 15d), 3d, reflective(gray(.9d)));
		// rtObject sphere5 =
		// sphere.c(v(0d, -20d, 30d), 3d, solid(v(0d, 0d, 0d)));

		var scene = new Scene(List.of(sphere0, sphere1, sphere2, sphere3, sphere4));

		var light0 = new PointLightSource(v(0d, -20d, 30d), gray(3d));
		var lights = lights(light0);

		var rayTracer = new RayTracer(lights, scene);
		rayTracer.setAmbient(gray(2d));

		rasterize(rayTracer);
	}

	@Test
	public void testLight() throws IOException {
		var sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(cw));
		var scene = new Scene(List.of(sky));

		var light = new PointLightSource(v(0d, 0d, 90d), cw);
		var lights = lights(light);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testMess() throws IOException {
		var sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		var sphere0 = Sphere.c(v(1d, -1d, 4d), 1d, glassy(cr));
		var sphere1 = Sphere.c(v(0d, 0d, 6d), 1d, glassy(cg));
		var sphere2 = Sphere.c(v(-1d, 1d, 8d), 1d, glassy(cb));
		var plane0 = new Plane(v(0d, -1d, 0d), 20d, solid(cy));
		var triangle = Triangle.c(v(.5d, .5d, 3d), v(.5d, 0d, 0d), v(0d, .5d, 0d), glassy(cc));
		var scene = new Scene(List.of(sky, sphere0, sphere1, sphere2, plane0, triangle));

		var light0 = new PointLightSource(v(10d, 10d, -10d), cp);
		var light1 = new PointLightSource(v(-10d, 10d, -10d), gray(2d));
		var lights = lights(light0, light1);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereMirror() throws IOException {
		var sphere = Sphere.c(v(0d, 0d, 3d), 1d, solid(cb));
		var mirror = new Plane(v(1d, 0d, 0d), -.3d, glassy(cw));
		var scene = new Scene(List.of(sphere, mirror));

		var light = new PointLightSource(v(10000d, 10000d, -10000d), gray(1.5d));
		var lights = lights(light);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereReflection() throws IOException {
		var sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(cw));
		var sphere = Sphere.c(v(0d, 0d, 3d), 1d, reflective(cb));
		var scene = new Scene(List.of(sky, sphere));

		var light = new PointLightSource(v(0d, 0d, 90d), cw);
		var lights = lights(light);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereRefraction() throws IOException {
		var sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(cw));
		var sphere = Sphere.c(v(0d, 0d, 3d), 1d, glassy(cb));
		var scene = new Scene(List.of(sky, sphere));

		var light = new PointLightSource(v(0d, 0d, 90d), cw);
		var lights = lights(light);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSphereSolid() throws IOException {
		var sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(cw));
		var sphere = Sphere.c(v(0d, 0d, 3d), 1d, solid(cb));
		var scene = new Scene(List.of(sky, sphere));

		var light = new PointLightSource(v(0d, 0d, 90d), cw);
		var lights = lights(light);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	@Test
	public void testSpheres() throws IOException {
		var sky = Sphere.c(v(0d, 0d, 0d), 100d, solid(gray(.4d)));
		var sphere0 = Sphere.c(v(-1.5d, 0d, 5d), 1d, glassy(cb));
		var sphere1 = Sphere.c(v(1.5d, 0d, 5d), 1d, glassy(cb));
		var scene = new Scene(List.of(sky, sphere0, sphere1));

		var light = new PointLightSource(v(0d, 0d, 5d), gray(1.5d));
		var lights = lights(light);

		var rayTracer = new RayTracer(lights, scene);
		rasterize(rayTracer);
	}

	private void rasterize(RayTracer rayTracer) throws IOException {
		var path = Defaults.tmp(Thread_.getStackTrace(3).getMethodName() + ".png");
		var bufferedImage = rayTracer.trace(640, 480, 640);
		if (Boolean.TRUE)
			FileUtil.out(path).doWrite(os -> ImageIO.write(bufferedImage, "png", os));
		else {
			new ImageViewer(bufferedImage);
			System.in.read();
		}
	}

	private List<LightSource> lights(LightSource... lights) {
		return Arrays.asList(lights);
	}

	private Material solid(R3 color) {
		return material(color, false, false);
	}

	private Material glassy(R3 color) {
		return material(color, true, true);
	}

	private Material reflective(R3 color) {
		return material(color, true, false);
	}

	private Material material(R3 color, boolean isReflective, boolean isTransparent) {
		return new Material() {
			public R3 surfaceColor() {
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

	private R3 gray(double f) {
		return v(f, f, f);
	}

	private R3 v(double x, double y, double z) {
		return new R3(x, y, z);
	}

}
