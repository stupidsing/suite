package suite.rt;

import suite.math.MathUtil;
import suite.math.Vector;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RayHitDetail;
import suite.rt.RayTracer.RayTraceObject;

public class Sphere implements RayTraceObject {

	private Vector centre;
	private float radius;

	public Sphere(Vector centre, float radius) {
		this.centre = centre;
		this.radius = radius;
	}

	@Override
	public RayHit hit(final Vector startPoint, final Vector direction) {
		Vector start0 = Vector.sub(startPoint, centre);
		float a = Vector.dot(direction, direction);
		float dist; // Distance the ray travelled, positive if hits

		if (a > MathUtil.epsilon) {
			float b = 2 * Vector.dot(start0, direction);
			float c = Vector.dot(start0, start0) - radius * radius;
			float discriminant = b * b - 4 * a * c;

			if (discriminant > 0) { // No hit
				float sqrt = (float) Math.sqrt(discriminant);

				if (-b - sqrt > 0)
					dist = (-b - sqrt) / (2 * a);
				else
					dist = (-b + sqrt) / (2 * a);
			} else
				dist = -1f;
		} else
			dist = -1f;

		final float distance = dist;

		if (distance > 0)
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
							return Vector.sub(hitPoint, centre);
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
