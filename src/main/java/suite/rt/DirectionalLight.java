package suite.rt;

import suite.math.Vector;
import suite.rt.RayTracer.LightSource;

public class DirectionalLight implements LightSource {

	private Vector source;
	private Vector color;

	public DirectionalLight(Vector source, Vector color) {
		this.source = source;
		this.color = color;
	}

	@Override
	public Vector source() {
		return source;
	}

	@Override
	public Vector lit(Vector startPoint, Vector direction) {
		Vector lightDirection = Vector.sub(source, startPoint);
		float factor = Vector.dot(direction, lightDirection)
				/ (float) Math.sqrt(Vector.normsq(lightDirection) * Vector.normsq(direction));
		return factor > 0 ? Vector.mul(color, factor) : Vector.origin;
	}

}
