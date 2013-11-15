package suite.rt;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import suite.math.Vector;

public class RayTracer {

	private Lighting lighting;
	private RayTraceObject scene;

	public interface RayTraceObject {
		public RayHit hit(Vector startPoint, Vector direction);
	}

	public interface RayHit {
		public float distance();

		public RayHitDetail detail();
	}

	public interface RayHitDetail {
		public Vector hitPoint();

		public Vector normal();

		public Vector reflectionIndex();

		public Vector refractionIndex();
	}

	public interface Lighting {
		public Vector lit(Vector startPoint, Vector direction);
	}

	public RayTracer(Lighting lighting, RayTraceObject scene) {
		this.lighting = lighting;
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
				Vector startPoint = Vector.origin;
				Vector direction = new Vector(x - centreX, y - centreY, viewDistance);
				Vector color = trace(depth, startPoint, direction);
				bufferedImage.setRGB(x, y, new Color(color.getX(), color.getY(), color.getZ()).getRGB());
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

			Vector normal = Vector.norm(d.normal());
			Vector reflectingDirection = Vector.add(direction, Vector.mul(normal, -2f * Vector.dot(direction, normal)));
			Vector reflecting = trace(depth - 1, hitPoint, reflectingDirection);
			color = multiplyComponents(reflecting, d.reflectionIndex());

			// TODO refraction
		} else
			color = lighting.lit(startPoint, direction);

		return color;
	}

	private static Vector multiplyComponents(Vector u, Vector v) {
		return new Vector(u.getX() * v.getX(), u.getY() * v.getY(), u.getZ() * v.getZ());

	}

}
