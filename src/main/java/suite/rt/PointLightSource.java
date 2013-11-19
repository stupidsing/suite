package suite.rt;

import suite.math.Vector;
import suite.rt.RayTracer.LightSource;

public class PointLightSource implements LightSource {

	private Vector source;
	private Vector lum;

	public PointLightSource(Vector source, Vector lum) {
		this.source = source;
		this.lum = lum;
	}

	@Override
	public Vector source() {
		return source;
	}

	@Override
	public Vector lit(Vector point) {
		return lum;
	}

}
