package suite.rt.planar;

import java.util.List;

import suite.math.MathUtil;
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
		double denum = R3.dot(normal, ray.dir);
		double adv;

		if (MathUtil.epsilon < Math.abs(denum))
			adv = (originIndex - R3.dot(normal, ray.startPoint)) / denum;
		else
			adv = -1d; // treats as not-hit

		double advance = adv;

		if (RayTracer.negligibleAdvance < advance) {
			RayHit rayHit = new RayHit() {
				public double advance() {
					return advance;
				}

				public RayIntersection intersection() {
					R3 hitPoint = ray.hitPoint(advance);

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
