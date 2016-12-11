package suite.rt.planars;

import java.util.Arrays;
import java.util.Collections;
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
	private float originIndex;
	private Material material;

	public Plane(Vector normal, float originIndex, Material material) {
		this.normal = normal;
		this.originIndex = originIndex;
		this.material = material;
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		float denum = Vector.dot(normal, ray.dir);
		float adv;

		if (MathUtil.epsilon < Math.abs(denum))
			adv = (originIndex - Vector.dot(normal, ray.startPoint)) / denum;
		else
			adv = -1f; // treats as not-hit

		float advance = adv;

		if (RayTracer.negligibleAdvance < advance) {
			RayHit rayHit = new RayHit() {
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
							return normal;
						}

						public Material material() {
							return material;
						}
					};
				}
			};

			return Arrays.asList(rayHit);
		} else
			return Collections.emptyList();
	}

}
