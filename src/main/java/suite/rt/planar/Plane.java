package suite.rt.planar;

import static java.lang.Math.abs;

import java.util.List;

import suite.math.Math_;
import suite.math.R3;
import suite.rt.RayTracer;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayIntersection;
import suite.rt.RayTracer.RtObject;

public class Plane implements RtObject {

	private R3 normal;
	private double originIndex;
	private Material material;

	public Plane(R3 normal, double originIndex, Material material) {
		this.normal = normal;
		this.originIndex = originIndex;
		this.material = material;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		var denum = R3.dot(normal, ray.dir);
		double adv;

		if (Math_.epsilon < abs(denum))
			adv = (originIndex - R3.dot(normal, ray.startPoint)) / denum;
		else
			adv = -1d; // treats as not-hit

		var advance = adv;

		if (RayTracer.negligibleAdvance < advance) {
			var rayHit = new RayHit() {
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
							return normal;
						}

						public Material material() {
							return material;
						}
					};
				}
			};

			return List.of(rayHit);
		} else
			return List.of();
	}

}
