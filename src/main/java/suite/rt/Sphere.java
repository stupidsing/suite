package suite.rt;

import static java.lang.Math.sqrt;

import java.util.List;

import suite.math.R3;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayIntersection;
import suite.rt.RayTracer.RtObject;

public class Sphere implements RtObject {

	private R3 center;
	private double radius;
	private Material material;

	public Sphere(R3 center, double radius, Material material) {
		this.center = center;
		this.radius = radius;
		this.material = material;
	}

	public static RtObject c(R3 center, double radius, Material material) {
		var radiusRange = new R3(radius, radius, radius);
		var min = R3.sub(center, radiusRange);
		var max = R3.add(center, radiusRange);
		return new BoundingBox(min, max, new Sphere(center, radius, material));
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		var start0 = R3.sub(ray.startPoint, center);
		var a = ray.dir.abs2();
		var b = 2d * R3.dot(start0, ray.dir);
		var c = start0.abs2() - radius * radius;
		var discriminant = b * b - 4f * a * c;
		List<RayHit> rayHits;

		if (0 < discriminant) { // hit?
			var sqrt = sqrt(discriminant);
			var denom = 1d / (2d * a);
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
				var hitPoint = ray.hitPoint(advance);

				return new RayIntersection() {
					public R3 hitPoint() {
						return hitPoint;
					}

					public R3 normal() {
						return R3.sub(hitPoint, center);
					}

					public Material material() {
						return material;
					}
				};
			}
		};
	}

}
