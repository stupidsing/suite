package suite.rt;

import suite.math.Vector;
import suite.rt.RayTracer.LightSource;
import suite.rt.RayTracer.Ray;

public class PointLight implements LightSource {

	private Vector source;
	private Vector color;

	public PointLight(Vector source, Vector color) {
		this.source = source;
		this.color = color;
	}

	@Override
	public Vector source() {
		return source;
	}

	@Override
	public Vector lit(Ray ray) {
		Vector lightDir = Vector.sub(ray.startPoint, source);
		float factor = Vector.dot(ray.dir, lightDir) / (float) Math.sqrt(Vector.normsq(lightDir) * Vector.normsq(ray.dir));
		return factor > 0 ? Vector.mul(color, factor) : Vector.origin;
	}

}
