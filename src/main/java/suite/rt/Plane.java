package suite.rt;

import suite.math.MathUtil;
import suite.math.Vector;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayHitDetail;
import suite.rt.RayTracer.RayTraceObject;

public class Plane implements RayTraceObject {

	private Vector normal;
	private float originIndex;

	public Plane(Vector normal, float originIndex) {
		this.normal = normal;
		this.originIndex = originIndex;
	}

	@Override
	public RayHit hit(final Vector startPoint, final Vector direction) {
		float denum = Vector.dot(normal, direction);
		float dist;

		if (Math.abs(denum) > MathUtil.epsilon)
			dist = -(Vector.dot(normal, startPoint) + originIndex) / denum;
		else
			dist = -1f; // Treats as not-hit

		final float distance = dist;

		if (distance > MathUtil.epsilon)
			return new RayHit() {
				public float distance() {
					return distance;
				}

				public RayHitDetail detail() {
					final Vector hitPoint = Vector.add(startPoint, Vector.mul(direction, distance));

					return new RayHitDetail() {
						public Vector hitPoint() {
							return hitPoint;
						}

						public Vector normal() {
							return normal;
						}

						public Vector reflectionIndex() {
							return new Vector(0.8f, 0.8f, 0.8f);
						}

						public Vector refractionIndex() {
							return new Vector(0.8f, 0.8f, 0.8f);
						}
					};
				}
			};
		else
			return null;
	}

}
