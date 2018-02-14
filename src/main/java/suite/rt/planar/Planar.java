package suite.rt.planar;

import java.util.ArrayList;
import java.util.List;

import suite.math.Vector;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RtObject;

public abstract class Planar implements RtObject {

	private Vector origin;
	private Vector axis0, axis1;
	private Plane plane;
	private double invAxis0, invAxis1;
	private IsHit isHit;

	public interface IsHit {
		public abstract boolean isHit(double x, double y);
	}

	public Planar(Vector origin, Vector axis0, Vector axis1, IsHit isHit, Material material) {
		this.origin = origin;
		this.axis0 = axis0;
		this.axis1 = axis1;
		this.isHit = isHit;

		Vector normal = Vector.cross(axis0, axis1);
		plane = new Plane(normal, Vector.dot(origin, normal), material);
		invAxis0 = 1f / Vector.abs2(axis0);
		invAxis1 = 1f / Vector.abs2(axis1);
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		List<RayHit> rayHits = new ArrayList<>();

		for (RayHit rayHit : plane.hit(ray)) {
			Vector planarDir = Vector.sub(rayHit.intersection().hitPoint(), origin);
			double x = Vector.dot(planarDir, axis0) * invAxis0;
			double y = Vector.dot(planarDir, axis1) * invAxis1;

			if (isHit.isHit(x, y))
				rayHits.add(rayHit);
		}

		return rayHits;
	}

}
