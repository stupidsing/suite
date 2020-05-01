package suite.rt.planar;

import suite.math.R3;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.Ray;
import suite.rt.RayTracer.RayHit;
import suite.rt.RayTracer.RtObject;

import java.util.ArrayList;
import java.util.List;

public abstract class Planar implements RtObject {

	private R3 origin;
	private R3 axis0, axis1;
	private Plane plane;
	private double invAxis0, invAxis1;
	private IsHit isHit;

	public interface IsHit {
		public abstract boolean isHit(double x, double y);
	}

	public Planar(R3 origin, R3 axis0, R3 axis1, IsHit isHit, Material material) {
		this.origin = origin;
		this.axis0 = axis0;
		this.axis1 = axis1;
		this.isHit = isHit;

		var normal = R3.cross(axis0, axis1);
		plane = new Plane(normal, R3.dot(origin, normal), material);
		invAxis0 = 1d / axis0.abs2();
		invAxis1 = 1d / axis1.abs2();
	}

	@Override
	public List<RayHit> hit(Ray ray) {
		var rayHits = new ArrayList<RayHit>();

		for (var rayHit : plane.hit(ray)) {
			var planarDir = R3.sub(rayHit.intersection().hitPoint(), origin);
			var x = R3.dot(planarDir, axis0) * invAxis0;
			var y = R3.dot(planarDir, axis1) * invAxis1;

			if (isHit.isHit(x, y))
				rayHits.add(rayHit);
		}

		return rayHits;
	}

}
