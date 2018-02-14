package suite.rt;

import java.util.List;

import suite.math.Vector;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayIntersection;
import suite.rt.RayTracer.RtObject;

public class Sphere implements RtObject {

	private Vector center;
	private double radius;
	private Material material;

	public Sphere(Vector center, double radius, Material material) {
		this.center = center;
		this.radius = radius;
		this.material = material;
	}

	public static RtObject c(Vector center, double radius, Material material) {
		Vector radiusRange = new Vector(radius, radius, radius);
		Vector min = Vector.sub(center, radiusRange);
		Vector max = Vector.add(center, radiusRange);
		return new BoundingBox(min, max, new Sphere(center, radius, material));
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		Vector start0 = Vector.sub(ray.startPoint, center);
		double a = Vector.abs2(ray.dir);
		double b = 2f * Vector.dot(start0, ray.dir);
		double c = Vector.abs2(start0) - radius * radius;
		double discriminant = b * b - 4f * a * c;
		List<RayHit> rayHits;

		if (0 < discriminant) { // hit?
			double sqrt = Math.sqrt(discriminant);
			double denom = 1d / (2d * a);
			rayHits = List.of(rayHit(ray, (-b - sqrt) * denom), rayHit(ray, (-b + sqrt) * denom));
		} else
			rayHits = List.of();

		return rayHits;
	}

	private RayHit rayHit(Ray ray, double advance) {
		return new RayHit() {
			public double advance() {
				return advance;
			}

			public RayIntersection intersection() {
				Vector hitPoint = ray.hitPoint(advance);

				return new RayIntersection() {
					public Vector hitPoint() {
						return hitPoint;
					}

					public Vector normal() {
						return Vector.sub(hitPoint, center);
					}

					public Material material() {
						return material;
					}
				};
			}
		};
	}

}
