package suite.rt.planar;

import suite.math.R3;
import suite.rt.BoundingBox;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.RtObject;

import java.util.List;

public class Triangle extends Planar implements RtObject {

	public Triangle(R3 origin, R3 axis0, R3 axis1, Material material) {
		super(origin, axis0, axis1, (x, y) -> 0f <= x && 0f <= y && x + y < 1f, material);
	}

	public static RtObject c(R3 origin, R3 axis0, R3 axis1, Material material) {
		var v0 = R3.add(origin, axis0);
		var v1 = R3.add(origin, axis1);
		var triangle = new Triangle(origin, axis0, axis1, material);
		return BoundingBox.bound(List.of(origin, v0, v1), triangle);
	}

}
