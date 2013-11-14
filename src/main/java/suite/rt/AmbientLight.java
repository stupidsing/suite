package suite.rt;

import suite.math.Vector;
import suite.rt.RayTracer.Lighting;

public class AmbientLight implements Lighting {

	private Vector lightDirection;
	private Vector color;

	public AmbientLight(Vector lightDirection, Vector color) {
		this.lightDirection = Vector.norm(lightDirection);
		this.color = color;
	}

	@Override
	public Vector lit(Vector startPoint, Vector direction) {
		float factor = Vector.dot(direction, lightDirection) / (float) Math.sqrt(Vector.dot(direction, direction));
		return factor > 0 ? Vector.mul(color, factor) : Vector.origin;
	}

}
