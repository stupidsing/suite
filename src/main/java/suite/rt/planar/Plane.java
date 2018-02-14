package suite.rt.planar;

import java.util.List;

import suite.math.MathUtil;
import suite.math.Vector;
import suite.rt.RayTracer;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayIntersection;
import suite.rt.RayTracer.RtObject;

public class Plane implements RtObject {

	private Vector normal;
	private double originIndex;
	private Material material;

	public Plane(Vector normal, double originIndex, Material material) {
		this.normal = normal;
		this.originIndex = originIndex;
		this.material = material;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		double denum = Vector.dot(normal, ray.dir);
		double adv;

		if (MathUtil.epsilon < Math.abs(denum))
			adv = (originIndex - Vector.dot(normal, ray.startPoint)) / denum;
		else
			adv = -1d; // treats as not-hit

		double advance = adv;

		if (RayTracer.negligibleAdvance < advance) {
			RayHit rayHit = new RayHit() {
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
