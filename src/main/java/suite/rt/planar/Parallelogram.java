package suite.rt.planar;

import suite.math.R3;
import suite.rt.BoundingBox;
import suite.rt.RayTracer.Material;
import suite.rt.RayTracer.RtObject;

import java.util.List;

public class Parallelogram extends Planar implements RtObject {

	public Parallelogram(R3 origin, R3 axis0, R3 axis1, Material material) {
		super(origin, axis0, axis1, (x, y) -> 0f <= x && x < 1f && 0f <= y && y < 1f, material);
	}

	public static RtObject c(R3 origin, R3 axis0, R3 axis1, Material material) {
		var v0 = R3.add(origin, axis0);
		var v1 = R3.add(origin, axis1);
		var v2 = R3.add(origin, R3.add(axis0, axis1));
		var parallelogram = new Parallelogram(origin, axis0, axis1, material);
		return BoundingBox.bound(List.of(origin, v0, v1, v2), parallelogram);
	}

}
