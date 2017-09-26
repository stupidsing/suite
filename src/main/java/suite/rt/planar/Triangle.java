package suite.rt.planar;

import java.util.List;

import suite.math.Vector;
import suite.rt.BoundingBox;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.RtObject;

public class Triangle extends Planar implements RtObject {

	public Triangle(Vector origin, Vector axis0, Vector axis1, Material material) {
		super(origin, axis0, axis1, (x, y) -> 0f <= x && 0f <= y && x + y < 1f, material);
	}

	public static RtObject c(Vector origin, Vector axis0, Vector axis1, Material material) {
		Vector v0 = Vector.add(origin, axis0);
		Vector v1 = Vector.add(origin, axis1);
		Triangle triangle = new Triangle(origin, axis0, axis1, material);
		return BoundingBox.bound(List.of(origin, v0, v1), triangle);
	}

}
