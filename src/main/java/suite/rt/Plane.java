package suite.rt;

import suite.math.MathUtil;
import suite.math.Vector;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayHitDetail;
import suite.rt.RayTracer.RayTraceObject;

public class Plane implements RayTraceObject {

	private Vector normal;
	private float originIndex;
	private Material material;

	public Plane(Vector normal, float originIndex, Material material) {
		this.normal = normal;
		this.originIndex = originIndex;
		this.material = material;
	}

	@Override
	public RayHit hit(final Ray ray) {
		float denum = Vector.dot(normal, ray.dir);
		float adv;

		if (Math.abs(denum) > MathUtil.epsilon)
			adv = (originIndex - Vector.dot(normal, ray.startPoint)) / denum;
		else
			adv = -1f; // Treats as not-hit

		final float advance = adv;

		if (advance > RayTracer.negligibleAdvance)
			return new RayHit() {
				public float advance() {
					return advance;
				}

				public RayHitDetail detail() {
					final Vector hitPoint = ray.hitPoint(advance);

					return new RayHitDetail() {
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
		else
			return null;
	}

}
