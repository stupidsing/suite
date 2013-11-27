package suite.rt;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import suite.math.Vector;
import suite.util.LogUtil;

public class RayTracer {

	public static final float negligibleAdvance = 0.0001f;

	private int nThreads = 4;
	private int depth = 4;
	private float refractiveIndexRatio = 1.1f;
	private float mix = 0.1f;
	private Vector ambient = Vector.origin;

	private Collection<LightSource> lightSources;
	private RayTrace scene;

	public interface RayTrace {

		/**
		 * Calculates hit point with a ray. Assumes direction is normalized.
		 */
		public List<RayHit> hit(Ray ray);
	}

	public interface RayHit {
		public float advance();

		public RayIntersection intersection();

		public Comparator<RayHit> comparator = new Comparator<RayHit>() {
			public int compare(RayHit rh0, RayHit rh1) {
				return rh0.advance() < rh1.advance() ? -1 : 1;
			}
		};
	}

	public interface RayIntersection {
		public Vector hitPoint();

		public Vector normal();

		public Material material();
	}

	public interface Material {
		public Vector surfaceColor();

		public float reflectionIndex();

		public float refractionIndex();
	}

	public static class Ray {
		public Vector startPoint;
		public Vector dir;

		public Ray(Vector startPoint, Vector dir) {
			this.startPoint = startPoint;
			this.dir = dir;
		}

		public Vector hitPoint(float advance) {
			return Vector.add(startPoint, Vector.mul(dir, advance));
		}
	}

	public interface LightSource {
		public Vector source();

		public Vector lit(Vector point);
	}

	public RayTracer(Collection<LightSource> lightSources, RayTrace scene) {
		this.lightSources = lightSources;
		this.scene = scene;
	}

	public Vector test() {
		return traceRay(depth, new Ray(Vector.origin, new Vector(0f, 0f, 1f)));
	}

	public void trace(BufferedImage bufferedImage, final int viewDistance) {
		final int width = bufferedImage.getWidth(), height = bufferedImage.getHeight();
		final int centreX = width / 2, centreY = height / 2;
		final Vector pixels[][] = new Vector[width][height];
		final int xs[] = new int[nThreads + 1];

		for (int i = 0; i <= nThreads; i++)
			xs[i] = width * i / nThreads;

		Thread threads[] = new Thread[nThreads];

		for (int i = 0; i < nThreads; i++) {
			final int i1 = i;

			threads[i1] = new Thread() {
				public void run() {
					for (int x = xs[i1]; x < xs[i1 + 1]; x++)
						for (int y = 0; y < height; y++) {
							Vector lit;

							try {
								Vector startPoint = Vector.origin;
								Vector dir = Vector.norm(new Vector(x - centreX, y - centreY, viewDistance));
								lit = traceRay(depth, new Ray(startPoint, dir));
							} catch (Exception ex) {
								LogUtil.error(new RuntimeException("at (" + x + ", " + y + ")", ex));
								lit = new Vector(1f, 1f, 1f);
							}

							pixels[x][y] = lit;
						}
				}
			};
		}

		for (int i = 0; i < nThreads; i++)
			threads[i].start();

		for (int i = 0; i < nThreads; i++)
			try {
				threads[i].join();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				Vector pixel = limit(pixels[x][y]);
				bufferedImage.setRGB(x, y, new Color(pixel.getX(), pixel.getY(), pixel.getZ()).getRGB());
			}
	}

	private Vector traceRay(int depth, Ray ray) {
		RayHit rayHit = nearestHit(scene.hit(ray));

		Vector color1;

		if (rayHit != null) {
			RayIntersection i = rayHit.intersection();
			Vector hitPoint = i.hitPoint();
			Vector normal = Vector.norm(i.normal());

			Material material = i.material();
			float reflectionIndex = material.reflectionIndex();
			float refractionIndex = material.refractionIndex();
			Vector color;

			float dot = Vector.dot(ray.dir, normal);
			boolean isInside = dot > 0f;

			if (isInside) {
				normal = Vector.neg(normal);
				dot = -dot;
			}

			if (depth > 0 && (reflectionIndex > 0f || refractionIndex > 0f)) {
				float cos = -dot / (float) Math.sqrt(Vector.normsq(ray.dir));

				// Account reflection
				Vector reflectDir = Vector.add(ray.dir, Vector.mul(normal, -2f * dot));
				Vector reflectPoint = Vector.add(hitPoint, Vector.mul(normal, negligibleAdvance));
				Vector reflectColor = traceRay(depth - 1, new Ray(reflectPoint, reflectDir));

				// Account refraction
				Vector refractColor;

				if (refractionIndex > 0f) {
					float eta = isInside ? refractiveIndexRatio : 1f / refractiveIndexRatio;
					float k = 1 - eta * eta * (1 - cos * cos);
					Vector refractDir = Vector.add(Vector.mul(ray.dir, eta), Vector.mul(normal, eta * cos - (float) Math.sqrt(k)));
					Vector refractPoint = Vector.add(hitPoint, Vector.mul(normal, -negligibleAdvance));
					refractColor = traceRay(depth - 1, new Ray(refractPoint, refractDir));
				} else
					refractColor = Vector.origin;

				float cos1 = 1 - cos;
				float fresnel = mix + cos1 * cos1 * cos1 * (1 - mix);
				color = Vector.add(Vector.mul(Vector.mul(reflectColor, reflectionIndex), fresnel),
						Vector.mul(Vector.mul(refractColor, refractionIndex), 1 - fresnel));
			} else {
				color = Vector.origin;

				// Account light sources
				for (LightSource lightSource : lightSources) {
					Vector lightDir = Vector.sub(lightSource.source(), hitPoint);
					float lightDot = Vector.dot(lightDir, normal);

					if (lightDot > 0) { // Facing the light
						Vector lightPoint = Vector.add(hitPoint, Vector.mul(normal, negligibleAdvance));
						RayHit lightRayHit = nearestHit(scene.hit(new Ray(lightPoint, lightDir)));

						if (lightRayHit == null || lightRayHit.advance() > 1f) {
							Vector lightColor = lightSource.lit(hitPoint);
							float cos = lightDot / (float) Math.sqrt(Vector.normsq(lightDir));
							color = Vector.add(color, Vector.mul(lightColor, cos));
						}
					}
				}
			}

			color1 = mc(color, material.surfaceColor());
		} else
			color1 = ambient;

		return color1;
	}

	private RayHit nearestHit(List<RayHit> rayHits) {
		List<RayHit> rayHits1 = RayUtil.filterRayHits(rayHits);
		return !rayHits1.isEmpty() ? Collections.min(rayHits1, RayHit.comparator) : null;
	}

	/**
	 * Multiply vector components.
	 */
	private static Vector mc(Vector u, Vector v) {
		return new Vector(u.getX() * v.getX(), u.getY() * v.getY(), u.getZ() * v.getZ());

	}

	/**
	 * Limit vector components between 0 and 1.
	 */
	private static Vector limit(Vector u) {
		return new Vector(limit(u.getX()), limit(u.getY()), limit(u.getZ()));
	}

	private static float limit(float f) {
		return Math.min(1f, Math.max(0f, f));
	}

}
