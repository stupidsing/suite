package suite.rt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suite.math.Vector;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayIntersection;
import suite.rt.RayTracer.RtObject;

public class Sphere implements RtObject {

	private Vector centre;
	private float radius;
	private Material material;

	public Sphere(Vector centre, float radius, Material material) {
		this.centre = centre;
		this.radius = radius;
		this.material = material;
	}

	public static RtObject c(Vector centre, float radius, Material material) {
		Vector radiusRange = new Vector(radius, radius, radius);
		Vector min = Vector.sub(centre, radiusRange);
		Vector max = Vector.add(centre, radiusRange);
		return new BoundingBox(min, max, new Sphere(centre, radius, material));
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		Vector start0 = Vector.sub(ray.startPoint, centre);
		float a = Vector.abs2(ray.dir);
		float b = 2 * Vector.dot(start0, ray.dir);
		float c = Vector.abs2(start0) - radius * radius;
		float discriminant = b * b - 4 * a * c;
		List<RayHit> rayHits;

		if (0 < discriminant) { // Hit?
			float sqrt = (float) Math.sqrt(discriminant);
			float denom = 1 / (2f * a);
			rayHits = Arrays.asList(rayHit(ray, (-b - sqrt) * denom), rayHit(ray, (-b + sqrt) * denom));
		} else
			rayHits = Collections.emptyList();

		return rayHits;
	}

	private RayHit rayHit(Ray ray, float advance) {
		return new RayHit() {
			public float advance() {
				return advance;
			}

			public RayIntersection intersection() {
				Vector hitPoint = ray.hitPoint(advance);

				return new RayIntersection() {
					public Vector hitPoint() {
						return hitPoint;
					}

					public Vector normal() {
						return Vector.sub(hitPoint, centre);
					}

					public Material material() {
						return material;
					}
				};
			}
		};
	}

}
