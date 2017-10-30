package suite.rt;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import suite.image.Render;
import suite.math.Vector;

/**
 * TODO fix RayTracerTest.testLight() etc cases black-out issues
 *
 * TODO test accurate Fresnel (and Schlick's approximation?)
 *
 * @author ywsing
 */
public class RayTracer {

	public static float negligibleAdvance = .0001f;

	private int depth = 4;

	private float airRefractiveIndex = 1f;
	private float glassRefractiveIndex = 1.1f;
	private float adjustFresnel = 0f;

	private Vector ambient = Vector.origin;

	private Collection<LightSource> lightSources;
	private RtObject scene;

	public interface RtObject {

		/**
		 * Calculates hit point with a ray. Assumes direction is normalized.
		 */
		public List<RayHit> hit(Ray ray);
	}

	public interface RayHit {
		public float advance();

		public RayIntersection intersection();

		public Comparator<RayHit> comparator = (rh0, rh1) -> rh0.advance() < rh1.advance() ? -1 : 1;
	}

	public interface RayIntersection {
		public Vector hitPoint();

		public Vector normal();

		public Material material();
	}

	public interface Material {
		public Vector surfaceColor();

		public boolean isReflective();

		public float transparency();
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

		public String toString() {
			return startPoint + " => " + dir;
		}
	}

	public interface LightSource {
		public Vector source();

		public Vector lit(Vector point);
	}

	public RayTracer(Collection<LightSource> lightSources, RtObject scene) {
		this.lightSources = lightSources;
		this.scene = scene;
	}

	public Vector test() {
		return test(new Ray(Vector.origin, new Vector(0f, 0f, 1f)));
	}

	public Vector test(Ray ray) {
		return traceRay(depth, ray);
	}

	public BufferedImage trace(int width, int height, int viewDistance) {
		float ivd = ((float) viewDistance) / width;

		return new Render().render(width, height, (x, y) -> {
			Vector dir = new Vector(x, y, ivd);
			return traceRay(depth, new Ray(Vector.origin, dir));
		});
	}

	private Vector traceRay(int depth, Ray ray) {
		RayHit rayHit = nearestHit(scene.hit(ray));
		Vector color1;

		if (rayHit != null) {
			RayIntersection i = rayHit.intersection();
			Vector hitPoint = i.hitPoint();
			Vector normal0 = Vector.norm(i.normal());

			float dot0 = Vector.dot(ray.dir, normal0);
			boolean isInside = 0f < dot0;
			Vector normal;
			float dot;

			if (!isInside) {
				normal = normal0;
				dot = dot0;
			} else {
				normal = Vector.neg(normal0);
				dot = -dot0;
			}

			Material material = i.material();
			boolean reflective = material.isReflective();
			float transparency = material.transparency();
			Vector color;

			if ((reflective || transparency < 0f) && 0 < depth) {
				float cos = -dot / (float) Math.sqrt(Vector.abs2(ray.dir));

				// account reflection
				Vector reflectDir = Vector.add(ray.dir, Vector.mul(normal, -2f * dot));
				Vector reflectPoint = Vector.add(hitPoint, negligible(normal));
				Vector reflectColor = traceRay(depth - 1, new Ray(reflectPoint, reflectDir));

				// account refraction
				float eta = isInside ? glassRefractiveIndex / airRefractiveIndex : airRefractiveIndex / glassRefractiveIndex;
				float k = 1f - eta * eta * (1f - cos * cos);
				Vector refractColor;

				if (0 <= k) {
					Vector refractDir = Vector.add(Vector.mul(ray.dir, eta / (float) Math.sqrt(Vector.abs2(ray.dir))),
							Vector.mul(normal, eta * cos - (float) Math.sqrt(k)));
					Vector refractPoint = Vector.sub(hitPoint, negligible(normal));
					refractColor = traceRay(depth - 1, new Ray(refractPoint, refractDir));
				} else
					refractColor = Vector.origin;

				// accurate Fresnel equation
				// float cos1 = (float) Math.sqrt(k);
				// float f0 = (eta * cos - cos1) / (eta * cos + cos1);
				// float f1 = (cos - eta * cos1) / (cos + eta * cos1);
				// float fresnel = (f0 * f0 + f1 * f1) / 2f;

				// schlick approximation
				boolean isDramaticMix = true;
				float r = (airRefractiveIndex - glassRefractiveIndex) / (airRefractiveIndex + glassRefractiveIndex);
				float mix = isDramaticMix ? .1f : r * r;
				float cos1 = 1f - cos;
				float cos2 = cos1 * cos1;
				float fresnel = mix + (1f - mix) * cos1 * cos2 * cos2;

				// fresnel is often too low. Mark it up for visual effect.
				float fresnel1 = adjustFresnel + fresnel * (1f - adjustFresnel);

				color = Vector.add(Vector.mul(reflectColor, fresnel1), Vector.mul(refractColor, (1f - fresnel1) * transparency));
			} else {
				color = Vector.origin;

				// account light sources
				for (LightSource lightSource : lightSources) {
					Vector lightDir = Vector.sub(lightSource.source(), hitPoint);
					float lightDot = Vector.dot(lightDir, normal);

					if (0f < lightDot) { // facing the light
						Vector lightPoint = Vector.add(hitPoint, negligible(normal));
						RayHit lightRayHit = nearestHit(scene.hit(new Ray(lightPoint, lightDir)));

						if (lightRayHit == null || 1f < lightRayHit.advance()) {
							Vector lightColor = lightSource.lit(hitPoint);
							float cos = lightDot / (float) Math.sqrt(Vector.abs2(lightDir));
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
		return RayHit_.filter(rayHits).minOrNull(RayHit.comparator);
	}

	/**
	 * Multiply vector components.
	 */
	private static Vector mc(Vector u, Vector v) {
		return new Vector(u.x * v.x, u.y * v.y, u.z * v.z);
	}

	private static Vector negligible(Vector v) {
		return new Vector(negligible(v.x), negligible(v.y), negligible(v.z));
	}

	private static float negligible(float f) {
		if (0f < f)
			return negligibleAdvance;
		else if (f < 0f)
			return -negligibleAdvance;
		else
			return 0f;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setAdjustFresnel(float adjustFresnel) {
		this.adjustFresnel = adjustFresnel;
	}

	public void setAmbient(Vector ambient) {
		this.ambient = ambient;
	}

}
