package suite.rt;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.imageio.ImageIO;

import suite.math.Vector;
import suite.util.LogUtil;

public class RayTracer {

	public static final float negligibleAdvance = 0.001f;

	private Collection<LightSource> lightSources;
	private RayTraceObject scene;

	public interface RayTraceObject {

		/**
		 * Calculates hit point with a ray. Assumes direction is normalized.
		 */
		public RayHit hit(Vector startPoint, Vector direction);
	}

	public interface RayHit {
		public float advance();

		public RayHitDetail detail();
	}

	public interface RayHitDetail {
		public Vector hitPoint();

		public Vector normal();

		public Vector litIndex();

		public Vector reflectionIndex();

		public Vector refractionIndex();
	}

	public interface LightSource {
		public Vector source();

		public Vector lit(Vector startPoint, Vector direction);
	}

	public RayTracer(Collection<LightSource> lightSources, RayTraceObject scene) {
		this.lightSources = lightSources;
		this.scene = scene;
	}

	public void trace(int sizeX, int sizeY) throws IOException {
		trace(sizeX, sizeY, sizeX); // Default view distance
	}

	public void trace(int width, int height, int viewDistance) throws IOException {
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int centreX = width / 2, centreY = height / 2;
		int depth = 4;

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				Color color;

				try {
					Vector startPoint = Vector.origin;
					Vector direction = new Vector(x - centreX, y - centreY, viewDistance);
					Vector lit = trace(depth, startPoint, direction);
					color = new Color(lit.getX(), lit.getY(), lit.getZ());
				} catch (Exception ex) {
					LogUtil.error(new RuntimeException("at (" + x + ", " + y + ")", ex));
					color = new Color(1f, 1f, 1f);
				}

				bufferedImage.setRGB(x, y, color.getRGB());
			}

		File file = new File("/tmp/ray-tracer.png");
		ImageIO.write(bufferedImage, "png", file);
	}

	private Vector trace(int depth, Vector startPoint, Vector direction) {
		Vector color;
		RayHit rayHit;

		if (depth > 0 && (rayHit = scene.hit(startPoint, direction)) != null) {
			RayHitDetail d = rayHit.detail();
			Vector hitPoint = d.hitPoint();

			Vector lightingColor = Vector.origin;

			for (LightSource lightSource : lightSources) {
				RayHit rayHit1 = scene.hit(hitPoint, Vector.sub(lightSource.source(), hitPoint));

				if (rayHit1 == null || rayHit1.advance() > 1f)
					lightingColor = Vector.add(lightingColor, lightSource.lit(startPoint, direction));
			}

			Vector normal = Vector.norm(d.normal());
			Vector reflectingDirection = Vector.add(direction, Vector.mul(normal, -2f * Vector.dot(direction, normal)));
			Vector reflectingColor = trace(depth - 1, hitPoint, reflectingDirection);

			color = Vector.add(multiplyComponents(lightingColor, d.litIndex()),
					multiplyComponents(reflectingColor, d.reflectionIndex()));

			// TODO refraction
		} else {
			color = Vector.origin;

			for (LightSource lightSource : lightSources)
				color = Vector.add(color, lightSource.lit(startPoint, direction));
		}

		return color;
	}

	private static Vector multiplyComponents(Vector u, Vector v) {
		return new Vector(u.getX() * v.getX(), u.getY() * v.getY(), u.getZ() * v.getZ());

	}

}
